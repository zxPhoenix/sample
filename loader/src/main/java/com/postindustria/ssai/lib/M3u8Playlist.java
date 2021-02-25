package com.postindustria.ssai.lib;

import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;

public class M3u8Playlist {

    public M3u8Playlist(URL context, String content) {
        this.context = context;
        this.data = content;
    }

    public M3u8Playlist(URL context, InputStream in, boolean parseAfterReading) {
        this.data = readFromInputStream(in);
        this.context = context;
        if (parseAfterReading) {
            this.parse();
        }
    }

    private static Logger log = (Logger) LoggerFactory.getLogger("appLogger");

    private static String unwrapString(String s, char c) {
        return s.replace(c, ' ').trim().replace(' ', c);
    }

    private static String tokenStringToEnumName(String s) {
        return s.replace('-', '_').trim();
    }

    public enum FileType {
        MASTER_PLAYLIST,
        MEDIA_PLAYLIST
    }

    public static final float MEDIA_DURATION_NONE = -1;

    public static final String INTEGER_REGEX = "^\\d+";
    public static final String ENTRY_REGEX = "#([1-9A-Z-]+)(:[@A-Za-z0-9-=,\"._ ]+)?";
    public static final String COMMENT_REGEX = "##(.+)?";

    public static final String ENTRY_MASTER = "#EXT-X-STREAM-INF";
    public static final String ENTRY_MEDIA = "#EXTINF";

    public static final String CSV_ATTRIBUTES_LIST_REGEX = "\\s*(.+?)\\s*=((?:\".*?\")|.*?)(?:,|$)";

    public static final String ENTRY_SPLIT_CHAR = ":";
    public static final String VALUES_SPLIT_CHAR = ",";
    public static final String ATTRIBUTES_SPLIT_CHAR = "=";
    public static final String RESOLUTION_SPLIT_CHAR = "x";

    public enum EntryType  {
        EXTM3U("EXTM3U"),
        EXT_X_VERSION("EXT-X-VERSION"),
        EXT_X_MEDIA_SEQUENCE("EXT-X-MEDIA-SEQUENCE"),
        EXT_X_TARGETDURATION("EXT-X-TARGETDURATION"),
        EXTINF("EXTINF"),
        EXT_X_STREAM_INF("EXT-X-STREAM-INF"),
        EXT_X_MEDIA("EXT-X-MEDIA"),
        EXT_X_BYTERANGE("EXT-X-BYTERANGE"),
        EXT_X_PLAYLIST_TYPE("EXT-X-PLAYLIST-TYPE"),
        EXT_X_ENDLIST("EXT-X-ENDLIST"),
        EXT_X_MAP("EXT-X-MAP"),
        EXT_X_DISCONTINUITY_SEQUENCE("EXT-X-DISCONTINUITY-SEQUENCE"),
        EXT_X_DISCONTINUITY("EXT-X-DISCONTINUITY"),
        EXT_X_CUE_OUT("EXT-X-CUE-OUT"),
        EXT_X_CUE_IN("EXT-X-CUE-IN");

        private final String token;

        EntryType(String token) {
            this.token = token;
        }

        @Override
        public String toString() {
            return this.token;
        }

        static EntryType fromString(String entryType) {
            // format first to allow input to begin with # and trailing whitespaces
            // also we replace the dash char (-) by underscores (_) in order to
            // be able to make these token valid enum values and identify types.
            entryType = unwrapString(tokenStringToEnumName(entryType), '#');
            try {
                EntryType e = EntryType.valueOf(entryType);
                return e;
            } catch(IllegalArgumentException ex) {
                throw new RuntimeException("Unknown entry type token: " + entryType);
            }
        }

        boolean hasURL() {
            switch (this) {
                case EXTINF:
                case EXT_X_BYTERANGE:
                case EXT_X_STREAM_INF:
                    return true;
                default:
                    return false;
            }
        }
    }

    public enum AttributeType {
        PROGRAM_ID("PROGRAM-ID"),
        BANDWIDTH("BANDWIDTH"),
        CODECS("CODECS"),
        RESOLUTION("RESOLUTION"),
        TYPE("TYPE"),
        GROUP_ID("GROUP-ID"),
        LANGUAGE("LANGUAGE"),
        URI("URI"),
        NAME("NAME"),
        AUDIO("AUDIO"),
        VIDEO("VIDEO"),
        DURATION("DURATION"),
        FRAME_RATE("FRAME-RATE"),
        SUBTILES("SUBTITLES"),
        CLOSED_CAPTIONS("CLOSED_CAPTIONS");

        private final String attribute;

        AttributeType(String attribute) {
            this.attribute = attribute;
        }

        @Override
        public String toString() {
            return this.attribute;
        }

        static AttributeType fromString(String attribute) {
            try {
                AttributeType a = AttributeType.valueOf(tokenStringToEnumName(attribute));
                return a;
            } catch(IllegalArgumentException ex) {
                throw new RuntimeException("Unknown attribute type token: " + attribute);
            }
        }
    }

    public static class Attribute {
        Attribute(String a) {
            String[] parsedAttribute = a.split(ATTRIBUTES_SPLIT_CHAR);
            if (parsedAttribute.length != 2) {
                throw new RuntimeException("Malformed attribute: " + a);
            }
            this.type = AttributeType.fromString(parsedAttribute[0]);
            this.value = parsedAttribute[1].trim();
        }

        public String toString() {
            return this.type + "=" + this.value;
        }

        public String getValue() {
            switch(this.type) {
                case URI:
                case LANGUAGE:
                case CODECS:
                case AUDIO:
                case VIDEO:
                case SUBTILES:
                case NAME:
                case FRAME_RATE:
                case GROUP_ID:
                case CLOSED_CAPTIONS:
                    return unwrapString(this.value, '"');
                default:
                    return this.value;
            }
        }

        private final AttributeType type;
        private String value;
    }

    public static class Entry {
        public Entry(String e) {
            if (!Entry.couldBe(e)) {
                throw new RuntimeException("Failed to parse malformed entry: " + e);
            }

            // pre: e can be #SOME-TOKEN:XXXX
            // or it can be #SOME-TOKEN and that's it
            String[] parsedEntry;
            if (e.contains(ENTRY_SPLIT_CHAR)) {
                parsedEntry = e.split(ENTRY_SPLIT_CHAR);
            } else {
                parsedEntry = new String[1];
                parsedEntry[0] = e;
            }
            // post: parsedEntry is {#SOME-TOKEN, XXXX} (as applicable)

            // set EntryType from first part (#SOME-TOKEN)
            // note: this method accepts token with #
            this.type = EntryType.fromString(parsedEntry[0]);

            ArrayList<String> valuesList = new ArrayList<>();

            // There are some comma-separated-values behind
            if (parsedEntry.length > 1) {
                String rawValues = parsedEntry[1];

                if (rawValues.contains(VALUES_SPLIT_CHAR)) { // CSV string (may be single value too, see Shaka media playlists)
                    Pattern p = Pattern.compile(CSV_ATTRIBUTES_LIST_REGEX);
                    Matcher m = p.matcher(rawValues);
                    while(m.find()) {
                        valuesList.add(
                                // Note: Match may still contain trailing comma, removing it here
                                unwrapString(m.group(), ',')
                        );
                    }

                    // Didn't match attributes list regex, must be single value with trailing comma
                    // Note: Unfortunately our regex above doesn't match both cases here (simple values and attributes with '=' but
                    // they also usually don't come combined.
                    if (valuesList.size() == 0) {
                        valuesList.add(unwrapString(rawValues, ','));
                    }

                } else { // non-CSV (can only be a single value, faster then matching regex) (maybe could be removed since handled above kind off)
                    valuesList.add(rawValues);
                }

                if (valuesList.size() == 1) {
                    //log.info("Parsed single value: " + valuesList.get(0));
                }
            }

            this.values = valuesList;
        }

        static boolean couldBe(String e) {
            if (e.matches(ENTRY_REGEX)) {
                String tag = e.replace("#", "");
                for (EntryType c : EntryType.values()) {
                    if (tag.startsWith(c.toString())) {
                        return true;
                    }
                }
            }
            return false;
        }

        static boolean couldBeMaster(String e) {
            return e.startsWith(ENTRY_MASTER);
        }

        static boolean couldBeMedia(String e) {
            return e.startsWith(ENTRY_MEDIA);
        }

        /**
         * @member Type of entry (#...)
         */
        public final EntryType type;

        /**
         * @member CSV strings array (values behind the `:`)
         */
        public final ArrayList<String> values;

        public int index = 0;

        /**
         *
         * @return Attributes array created from current CSV strings array (values)
         */
        Attribute[] readAttributes() {
            if (values.size() < 1) {
                // better to return an empty array, no need to handle special cases for consumers
                //return new Attribute[0];
                return null;
            }

            List<Attribute> list = new ArrayList<Attribute>();
            for (int i = 0; i < values.size(); i++) {
                try {
                    Attribute a = new Attribute(values.get(i));
                    list.add(a);
                }
                catch(Exception e) {
                    log.warn("Unknown attribute: " + values.get(i));
                }
            }
            return list.toArray(new Attribute[0]);
        }

        void writeAttributes(Attribute[] attributes) {

        }

        public String checkSum() {
            String hash = "";
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("MD5");
                md.update(this.toString().getBytes());
                byte[] digest = md.digest();
                hash = DatatypeConverter.printHexBinary(digest).toLowerCase();
            } catch (Exception e) {
                log.error("Checsum error", e);
            }
            return hash;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append('#');
            sb.append(this.type.token);

            if (this.values.size() > 0) {
                sb.append(':');
            }

            for (String value : this.values) {
                sb.append(value);
                sb.append(',');
            }

            return sb.toString();
        }
    }

    public class GroupInfoEntry extends Entry {
        GroupInfoEntry(String e) {
            super(e);

            Attribute[] attributes = this.readAttributes();
            if (attributes == null) return;

            for (Attribute a: attributes) {
                switch (a.type) {
                    case GROUP_ID:
                        this.groupId = a.getValue();
                        break;
                    case NAME:
                        this.name = a.getValue();
                        break;
                    case LANGUAGE:
                        this.language = a.getValue();
                        break;
                    case URI:
                        // Q: Do we need to sign this URL too? And should we make this member
                        //    a URL object i.e make this class a URLEntry in this case (since we'd need some context to resolve it)
                        try {
                            this.uri = M3u8Playlist.this.context == null ? a.getValue() : new URL(M3u8Playlist.this.context, a.getValue()).toString();
                        } catch (MalformedURLException ex) {
                            ex.printStackTrace();
                        }
                        break;
                    case TYPE:
                        this.groupType = GroupType.fromString(a.value);
                        break;
                    default:
                        // Maybe we should not throw here as in principle there can be all sorts of attributes
                        // we may not need but we should at least log an error if that occurs...
                        throw new RuntimeException("Unknown attribute type: " + a.type);
                }
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('#');
            sb.append(this.type.token);

            if (this.values.size() > 0) {
                sb.append(':');
            }

            for (String value : this.values) {
                if(value.contains("URI")) {
                    sb.append("URI=\"" + getHash() + ".m3u8\"");
                } else
                    sb.append(value);
                sb.append(',');
            }

            return sb.toString();
            //throw new RuntimeException("not implemented");
        }

        String getHash() {
            MessageDigest digest = null;
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            byte[] encodedhash = digest.digest((this.uri).toString().getBytes(StandardCharsets.UTF_8));
            return DatatypeConverter.printHexBinary(encodedhash).toLowerCase();
        }

        private String groupId = null;
        private String name = null;
        private String language = null;
        public String uri = null;
        private GroupType groupType = null;
    }

    public static enum GroupType {
        AUDIO, VIDEO, SUBTITLES, CLOSED_CAPTIONS;

        static GroupType fromString(String s) {
            switch(s) {
                case "AUDIO": return AUDIO;
                case "VIDEO": return VIDEO;
                case "SUBTITLES": return SUBTITLES;
                case "CLOSED-CAPTIONS": return CLOSED_CAPTIONS;
                default: throw new RuntimeException("Invalid group type value: " + s);
            }
            //return GroupType.valueOf(s.trim());
        }
    }

    public static class URLEntry extends Entry {
        URLEntry(String e, URL url) {
            super(e);

            this.url = url;
        }

        // Note: This will be absolute. Important to relativize this back (via URI class) against the context of this file
        //       when we serialize
        public URL url;

        void setUrl(URL url) {
            this.url = url;
        }

        URL getUrl() {
            return this.url;
        }

        String getHash() {
            MessageDigest digest = null;
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            byte[] encodedhash = digest.digest(this.url.toString().getBytes(StandardCharsets.UTF_8));
            return DatatypeConverter.printHexBinary(encodedhash).toLowerCase();
        }

        @Override
        public String toString() {
            String entry =  super.toString();
            // entry should end with a line-break char
            return entry + '\n' + this.url.toString();
        }
    }

    public static class StreamInfoEntry extends URLEntry {
        public StreamInfoEntry(String e, URL url) {
            super(e, url);

            Attribute[] attributes = this.readAttributes();
            if (attributes == null) return;

            for (Attribute a: attributes) {
                switch (a.type) {
                    case PROGRAM_ID:
                        this.programId = Integer.parseUnsignedInt(a.value, 10);
                        break;
                    case BANDWIDTH:
                        this.bandwidth = Integer.parseUnsignedInt(a.value, 10);
                        break;
                    case FRAME_RATE:
                        this.frameRate = Integer.parseUnsignedInt(a.value.replace(",","").replace(".",""), 10);
                        break;
                    case CODECS:
                        this.codecs = a.getValue();
                        this.codecsList = Codec.listFromString(this.codecs);
                        break;
                    case RESOLUTION:
                        this.resolution = Resolution.fromString(a.value);
                        break;
                    case AUDIO:
                        this.audioGroupId = a.getValue();
                        break;
                    case VIDEO:
                        this.videoGroupId = a.getValue();
                        break;
                    case SUBTILES:
                        this.subtitlesGroupId = a.getValue();
                        break;
                    case NAME:
                        this.name = a.getValue();
                        break;
                    default:
                        // Maybe we should not throw here as in principle there can be all sorts of attributes
                        // we may not need but we should at least log an error if that occurs...
                        //throw new RuntimeException("Unknown attribute type: " + a.type);
                        log.error("Unknown attribute type: " + a.type, e);
                }
            }
        }

        @Override
        public String toString() {
            Attribute[] attributes = this.readAttributes();
            List<String> list = new ArrayList<>();
            if (attributes != null) {
                for (Attribute a: attributes) {
                    list.add(a.toString());
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append('#');
            sb.append(this.type.token);
            sb.append(':');
            sb.append(String.join(", ", list));
            sb.append('\n');
            String name = this.getHash() + ".m3u8";
            sb.append(name);
            return sb.toString();
        }

        private int programId = 0;
        private int bandwidth = 0;
        private int frameRate = 0;
        private String codecs = null;
        private String name = null;
        private String audioGroupId = null;
        private String videoGroupId = null;
        private String subtitlesGroupId = null;
        private Resolution resolution = null;
        private ArrayList<Codec> codecsList = null;

        public int getProgramId() {
            return programId;
        }

        public int getBandwidth() {
            return bandwidth;
        }

        public int getFrameRate() {
            return frameRate;
        }

        public String getCodecs() {
            return codecs;
        }

        public String getName() {
            return name;
        }

        public String getAudioGroupId() {
            return audioGroupId;
        }

        public String getVideoGroupId() {
            return videoGroupId;
        }

        public String getSubtitlesGroupId() {
            return subtitlesGroupId;
        }

        public Resolution getResolution() {
            return resolution;
        }

        public ArrayList<Codec> getCodecsList() {
            return codecsList;
        }
    }

    public static class HeaderInfoEntry extends Entry {
        public HeaderInfoEntry(String e) {
            super(e);

            if (this.values.size() != 0) {
                String longNumber = this.values.get(0);
                String[] parts;
                if (longNumber.contains(ENTRY_SPLIT_CHAR)) {
                    parts = longNumber.split(ENTRY_SPLIT_CHAR);
                } else {
                    parts = new String[1];
                    parts[0] = longNumber;
                }

                this.value = Long.parseLong(parts[0]);
            }
        }

        private long value = 0;
    }

    public static class MediaInfoEntry extends URLEntry {
        public boolean isIndependendSegment = false;
        public long discontinueShift = 0;
        MediaInfoEntry(String e, URL url) {
            super(e, url);

            if (this.values.size() != 1) {
                throw new RuntimeException("Entry should have exactly one value");
            }

            String floatNumber = this.values.get(0);
            String[] parts;
            if (floatNumber.contains(VALUES_SPLIT_CHAR)) {
                parts = floatNumber.split(VALUES_SPLIT_CHAR);
            } else {
                parts = new String[1];
                parts[0] = floatNumber;
            }

            this.duration = Float.parseFloat(parts[0]);
        }

        int addByteRange(Entry e, int offset) {
            if (e.values.size() != 1) {
                throw new RuntimeException("Entry should only have one value");
            }

            String byteRange = e.values.get(0);
            String[] byteRangeParsed = byteRange.split("@");

            if(byteRangeParsed.length > 1) {
                // TODO check spec if we need to accumulate or replace here (what is assumed is the latter)
                offset = Integer.parseUnsignedInt(byteRangeParsed[1]);
            }

            this.byteRangeStart = offset;
            // we need to add this to the offset, then subtract one because
            // this number is meant to be the "end" of the range, so the last byte index inclusively
            // whereas the number we parse here is an amount of bytes in the range, and
            // the offset is also inclusive.
            this.byteRangeEnd = offset + Integer.parseUnsignedInt(byteRangeParsed[0]) - 1;

            return this.byteRangeEnd;
        }

//        @Override
//        public String toString() {
//            String entry = "";
//            String temp = EntryType.EXTINF + ":" + duration;
//            entry += temp + "\n";
//            if (this.byteRangeStart < this.byteRangeEnd) {
//                // TODO optimization for serialization output size
//                // we could use the "compressed" way to pass on only byte-range lengths
//                // based on the previous offset as an assumed start but for this we would
//                // need the previous entry context here.
//                temp = EntryType.EXT_X_BYTERANGE + ":" + (this.byteRangeEnd - this.byteRangeStart + 1) + "@" + this.byteRangeStart + "\n";
//                entry += temp + "\n";
//            }
//
//            // entry should end with a line-break char
//            return entry + super.toString();
//        }

        public float duration = MEDIA_DURATION_NONE;
        private int byteRangeStart = 0;
        private int byteRangeEnd = -1;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            if(isIndependendSegment) {
                sb.append("#EXT-X-DISCONTINUITY\n");
            }

            String entry = sb.toString() + super.toString();
            // entry should end with a line-break char
            return entry;
        }
    }

    public static class AdInfoEntry extends Entry {
        AdInfoEntry(String e, URL url) {
            super(e);

            if (this.values.size() != 0) {
                Attribute[] attributes = this.readAttributes();
                if (attributes == null) return;

                for (Attribute a : attributes) {
                    switch (a.type) {
                        case DURATION:
                            this.duration = Float.parseFloat(a.value);
                            break;
                        default:
                            throw new RuntimeException("Unknown attribute type: " + a.type);
                    }
                }
            }
        }

        public float duration = MEDIA_DURATION_NONE;
    }

    public static class Resolution {

        static Resolution fromString(String res) {
            String[] parsedRes = res.split(RESOLUTION_SPLIT_CHAR);
            if (!(parsedRes.length == 2 && parsedRes[0].matches(INTEGER_REGEX) && parsedRes[1].matches(INTEGER_REGEX))) {
                throw new RuntimeException("Malformed resolution: " + res);
            }
            return new Resolution(
                    Integer.parseUnsignedInt(parsedRes[0]),
                    Integer.parseUnsignedInt(parsedRes[1])
            );
        }

        private final int width;
        private final int height;

        Resolution(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() {
            return width + RESOLUTION_SPLIT_CHAR + height;
        }
    }

    public enum CodecId {
        H264_HIGH_PROFILE_41,
        H264_HIGH_PROFILE_40,
        H264_HIGH_PROFILE_31,
        H264_MAIN_PROFILE_40,
        H264_MAIN_PROFILE_31,
        H264_MAIN_PROFILE_30,
        H264_BASE_PROFILE_31,
        H264_BASE_PROFILE_30,
        H264_BASE_PROFILE_21,
        AAC_LC,
        AAC_HE,
        MP3,
        NOT_IMPLEMENTED;

        static CodecId fromString(String codecString) {
            /**
             * @see https://developer.apple.com/library/content/documentation/NetworkingInternet/Conceptual/StreamingMediaGuide/FrequentlyAskedQuestions/FrequentlyAskedQuestions.html
             * @see https://cconcolato.github.io/media-mime-support/
             *
             */
            switch(codecString) {
                case "mp4a.40.2":
                    return CodecId.AAC_LC;
                case "mp4a.40.5":
                    return CodecId.AAC_HE;

                case "mp4a.40.34":
                    return CodecId.MP3;

                case "avc1.640029":
                    return CodecId.H264_HIGH_PROFILE_41;
                case "avc1.640028":
                    return CodecId.H264_HIGH_PROFILE_40;
                case "avc1.64001f":
                    return CodecId.H264_HIGH_PROFILE_31;

                case "avc1.4d0028":
                    return CodecId.H264_MAIN_PROFILE_40;
                case "avc1.4d001f":
                case "avc1.4d401f": // "constrained" mode
                    return CodecId.H264_MAIN_PROFILE_31;
                case "avc1.4d001e":
                case "avc1.77.30": // iOS v3 compat
                    return CodecId.H264_MAIN_PROFILE_30;

                case "avc1.42001f":
                    return CodecId.H264_BASE_PROFILE_31;
                case "avc1.42001e":
                case "avc1.66.30": // iOS v3 compat
                    return CodecId.H264_BASE_PROFILE_30;
                case "avc1.420016":
                    return CodecId.H264_BASE_PROFILE_21;

                // TODO: Add H265 profiles
                default:
                    return CodecId.NOT_IMPLEMENTED;
            }
        }
    }

    public static class Codec {

        static ArrayList<Codec> listFromString(String codecsString) {
            ArrayList<Codec> codecs = new ArrayList<Codec>();
            String[] parsedCodecs = codecsString.split(",");
            for (String c : parsedCodecs) {
                codecs.add(new Codec(c));
            }
            return codecs;
        }

        public Codec(String codecString) {
            this.id = CodecId.fromString(codecString);
        }

        public Codec(CodecId codecId) {
            this.id = codecId;
        }

        public boolean isAVC() {
            return this.id.name().startsWith("H264");
        }

        public boolean isAAC() {
            return this.id.name().startsWith("AAC");
        }

        public boolean isMP3() {
            return this.id.name().startsWith("MP3");
        }

        private CodecId id;
    }

    public static class ParsingState {
        Entry entry = null;
        StreamInfoEntry streamInfo = null;
        HeaderInfoEntry headerInfo = null;
        MediaInfoEntry mediaInfo = null;
        GroupInfoEntry groupInfo = null;
        AdInfoEntry adInfo = null;
        URLEntry urlEntry = null;
        URL url = null;
        boolean expectUrl = false;
    }

    static String readFromInputStream(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private final String data; // Should actually save ref to in stream
    private final URL context;
    private FileType fileType = null;
    private int index = 0;

    private final ArrayList<Entry> entries = new ArrayList<>();
    private final ArrayList<StreamInfoEntry> streamInfoEntries = new ArrayList<>();
    private final ArrayList<HeaderInfoEntry> headerInfoEntries = new ArrayList<>();
    private final ArrayList<MediaInfoEntry> mediaInfoEntries = new ArrayList<>();
    private final ArrayList<GroupInfoEntry> groupInfoEntries = new ArrayList<>();
    private final ArrayList<AdInfoEntry> adInfoEntries = new ArrayList<>();

    public ArrayList<Entry> getEntries() {
        return entries;
    }
    public ArrayList<StreamInfoEntry> getStreamInfoEntries() {
        return streamInfoEntries;
    }
    public ArrayList<HeaderInfoEntry> getHeaderEntries() {
        return headerInfoEntries;
    }
    public ArrayList<AdInfoEntry> getAdEntries() {
        return adInfoEntries;
    }
    public ArrayList<MediaInfoEntry> getMediaInfoEntries() {
        return mediaInfoEntries;
    }

    public void writeTo(java.io.OutputStream out) {
        PrintWriter pw = new PrintWriter(out);

        for (Entry e: this.entries) {
            String entry = e.toString();
            pw.write(entry);
            pw.write('\n');
            pw.flush();
        }
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();

        for (Entry e: this.entries) {
            String entry = e.toString();
            sw.write(entry);
            sw.write('\n');
            sw.write('\n');
        }

        return sw.toString();
    }

    /**
     * @return True if anything useful could be parsed. When this returns true and parse() is called,
     * the latter results in a no-op. We assume the initial input stream to be readable in finite time.
     */
    public boolean isParsed() {
        return this.entries.size() > 0 && this.fileType != null;
    }

    public boolean isMasterPlaylist() {
        Scanner s = new Scanner(this.data);
        while(s.hasNextLine()) {
            String line = s.nextLine();
            if (Entry.couldBeMaster(line)) {
                return true;
            }
        }
        return false;
    }

    public boolean isMediaPlaylist() {
        Scanner s = new Scanner(this.data);
        while(s.hasNextLine()) {
            String line = s.nextLine();
            if (Entry.couldBeMedia(line)) {
                return true;
            }
        }
        return false;
    }

    public long getChunkDuration() {
        if (this.isParsed() && this.headerInfoEntries.size() != 0) {
            Optional<HeaderInfoEntry> entry = this.headerInfoEntries.stream().filter(e -> e.type.equals(EntryType.EXT_X_TARGETDURATION)).findFirst();
            if (entry != null && entry.isPresent()) {
                return entry.get().value;
            }
        }

        return 0;
    }

    public long getSequence() {
        if (this.isParsed() && this.headerInfoEntries.size() != 0) {
            Optional<HeaderInfoEntry> entry = this.headerInfoEntries.stream().filter(e -> e.type.equals(EntryType.EXT_X_MEDIA_SEQUENCE)).findFirst();
            if (entry != null && entry.isPresent()) {
                return entry.get().value;
            }
        }

        return 0;
    }

    /**
     * Parses the input stream until the end. We assume the input stream to have an end.
     * Does something when isParsed returns false, otherwise returns immediately.
     * If parse did actually find anything useful, isParsed() will return true and further calls will be no-ops.
     */
    public void parse() {

        if (this.isParsed()) {
            return;
        }

        // TODO: make scanner read from stream progressively
        Scanner s = new Scanner(this.data);

        int byteRangeOffset = 0;
        ParsingState state = new ParsingState();

        while(s.hasNextLine()) {
            String line = s.nextLine();

            if (state.expectUrl && !Entry.couldBe(line)) { // Should be a URL now here
                try {
                    state.url = this.context == null ? new URL(line) : new URL(this.context, line);
                } catch(MalformedURLException mue) {
                    throw new RuntimeException("Expected URL but got: " + line + ". Are we missing the context?");
                }

                // If we parsed a URL entry, enrich it with that
                if(state.urlEntry != null) {
                    state.urlEntry.setUrl(state.url);
                } else { // Else is an error
                    throw new RuntimeException("Have parsed URL but no corresponding entry exists");
                }

                // Reset parser state and jump to next line
                this.digestParsingState(state);
                state = new ParsingState();
                continue;

            } else if (state.expectUrl && Entry.couldBe(line)) { // We wait for URL but comes another entry
                Entry urlInfoEntry = new Entry(line);

                switch(urlInfoEntry.type) {
                    case EXT_X_BYTERANGE:
                        if (state.mediaInfo == null) {
                            throw new RuntimeException("Assertion failed: An media info entry should be parsed before we read a byte-range entry");
                        }
                        byteRangeOffset = state.mediaInfo.addByteRange(urlInfoEntry, byteRangeOffset);
                        break;
                    default:
                        break;
                }
            } else if (!state.expectUrl && Entry.couldBe(line)) { // A plain and slate entry
                state.entry = new Entry(line);

                switch(state.entry.type) {
                    case EXT_X_VERSION:
                    case EXT_X_MEDIA_SEQUENCE:
                    case EXT_X_DISCONTINUITY_SEQUENCE:
                    case EXT_X_TARGETDURATION:
                        state.entry = state.headerInfo = new HeaderInfoEntry(line);
                        break;
                    case EXTINF:
                        state.entry = state.urlEntry = state.mediaInfo = new MediaInfoEntry(line, state.url);
                        break;
                    case EXT_X_STREAM_INF:
                        state.entry = state.urlEntry = state.streamInfo = new StreamInfoEntry(line, state.url);
                        break;
                    case EXT_X_MEDIA:
                        state.entry = state.groupInfo = new GroupInfoEntry(line);
                        break;
                    case EXT_X_DISCONTINUITY:
                    case EXT_X_CUE_OUT:
                    case EXT_X_CUE_IN:
                        state.entry = state.adInfo = new AdInfoEntry(line, state.url);
                    default:
                        break;
                }

                if (state.entry.type.hasURL()) {
                    state.expectUrl = true;
                } else {
                    this.digestParsingState(state);
                    state = new ParsingState();
                }
            } else { // Not an entry
                if (line.length() > 0 && !line.matches(COMMENT_REGEX)) {
                    //throw new RuntimeException("Line is not a valid entry: " + line);
                }
            }
        }
    }

    public boolean addTrailerToEachURL(String trailer) {
        for (Entry e : this.entries) {
            if (e instanceof URLEntry) {
                URLEntry urlEntry = (URLEntry) e;

                String url = urlEntry.getUrl().toString();
                URL newUrl;
                try {
                    newUrl = new URL(url + trailer);
                } catch(MalformedURLException mue) {
                    return false;
                }
                urlEntry.setUrl(newUrl);
            }
        }
        return true;
    }

    private void digestParsingState(ParsingState state) {
        state.entry.index = this.index;
        this.entries.add(state.entry);

        if (state.groupInfo != null) {
            this.digestFileType(FileType.MASTER_PLAYLIST);
            this.groupInfoEntries.add(state.groupInfo);
        }

        if (state.streamInfo != null) {
            this.digestFileType(FileType.MASTER_PLAYLIST);
            this.streamInfoEntries.add(state.streamInfo);
        }

        if (state.headerInfo != null) {
            this.digestFileType(FileType.MEDIA_PLAYLIST);
            this.headerInfoEntries.add(state.headerInfo);
        }

        if (state.mediaInfo != null) {
            this.digestFileType(FileType.MEDIA_PLAYLIST);
            this.mediaInfoEntries.add(state.mediaInfo);
        }

        if (state.adInfo != null) {
            this.digestFileType(FileType.MEDIA_PLAYLIST);
            this.adInfoEntries.add(state.adInfo);
        }

        this.index++;
    }

    private void digestFileType(FileType t) {
        if(this.fileType != null && t != this.fileType) {
            throw new RuntimeException("The file-type (master/media) of the m3u8 is ambiguous because of its content");
        }
        else if (this.fileType == null) {
            this.fileType = isMasterPlaylist()? FileType.MASTER_PLAYLIST : FileType.MEDIA_PLAYLIST;
        }
    }
}
