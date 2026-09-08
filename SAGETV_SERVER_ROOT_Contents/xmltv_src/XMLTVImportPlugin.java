/*
 * Copyright 2016 The XMLTVImportPlugin for SageTV Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
 /* Change Log
##V2.0  01/13/2022
1.  Add code if display-name or channel id number(example 5, 5.1 5-1) then add as the channel number for import.  Examples below import as channel 2.1 and 1162 respectively below
##2.02  01/14/2022
1.  Fixed first channel * bug
2.  Fixed double year in title when title already had a date
3.  add tms filed for episode-num instead of creating unique id
4.  Do not generate SxEx if default case is found ("onscreen", "common")
5.  Fix Display for SxEx in descriptions
6.  Fix short name being set as channel number
7.  Start code for reading Logo Icon for channel (WIP) 
##2.03  01/14/2022
1.  Fixed new Integer((int) to remove compile deprecated warning
2.  Switched org.xml.sax.helpers.XMLReaderFactory to javax.xml.parsers.SAXParserFactory; to remove compile deprecated warning
##2.04 01/16/2022
1.  Added .properties entry for xmltv.channel.display-name.ShortNameIndex 
		 default=0 with no entry .properties file
		 init.ChannelShortNameIndex0:=shortest found without spaces
		 init.ChannelShortNameIndex>0: XMLTV file index found in order starting with 1 <display-name>CBS</display-name>
2.  Added .properties entry for "xmltv.channel.xmltv.channel.ShortNameRegEx"		
		default=.* with no entry .properties file will find anything
3.  Added .properties entry for xmltv.channel.display-name.LongNameIndex 
		default=0 with no entry .properties file
		init.ChannelLongNameIndex0:=longest found with spaces
		init.ChannelLongNameIndex>0: XMLTV file index found in order starting with 1 <display-name>CBS</display-name>
4.  Added .properties entry for xmltv.channel.NumberTag 
		 default="" No entry .properties file.  
		 init.ChannelNumberTag="" None will try to detect channel number from <channel id=XXX> or <display-name>XXX<display-name>
		 init.ChannelNumberTag=channel then <channel id=XXX> 
		 init.ChannelNumberTag=TAG_NAME then <TAG_NAME>xxx<TAG_NAME>
5.  Added .properties entry for init.ChannelNumberTagIndex		
		default=0 with no entry .properties file
		init.ChannelNumberTagIndex=0 None will try to detect channel number from <channel id=xxx> or <display-name>
		init.ChannelNumberTagIndex>0 XMLTV file index found in order starting with 1
6.  Added .properties entry for "xmltv.channel.xmltv.channel.NumberTagRegEx"		
		default=.* with no entry .properties file will find anything
7.  Added .properties entry for "sagetv.channel.IconDownload"
		 default=false with no entry .properties
		 false=Do Note download image file
		 true=Download image file from channel <icon src= >
##2.06 01/18/2022
1.  Added the following .properties entry  for enabling or disabling logging parts.  Defualt with no entry is true
		log.show=false
		log.channel=false
		log.configuration=false
		log.defaults=false
2.	Changed the way ChannelId is calulated. The previous way caused collections.  The new way uses the ProviderID as part of the encoding. The new limit limits the Provider ID to 998. 
		 2147483647	Max Number
		 1000000000
		 1030002100
		      99999	SD MAX it Sends
		  998           Upper Part is Listing ProviderID 1-998 999 used if none provided (* 1000000)
		     123456     Max XMLTV Number without Decimal 999999   
		 1   123456     Max XMLTV Number with Decimal 9999.99 When Decimal (* 100 +1000000000)  
3.  Remove non file characters so Channel icons could be downloaded the following examples where failing: "Cats 24/7" or "Dogs 24/7"	
##2.08 01/28/2022
1.  Filter if desc tag is from channel section.  This would crash the plugin if not done.
2.  Add show if it is up to 8 hours previous. Fixes on first import no data problem
3.  Add setting this.show.showId on <episode-num system="pluto"> tag
4.  Fixed freeFormEpisodeNumber on <episode-num system="onscreen"> and <episode-num system="common"> tags
5.  Add new classs to hold all the init. variables from .properties file
6.  Added sagetv.channel.NumberOffset to offset channel numbers on import.
7.  Added if *.
 is found in server directory use it as properties without requirement of configurations= in xmltv.properties file
 ##2.09 11/13/2022
1. Added if no <date> tag is found use <start> time.  This means it is new or live
2. Switch function from addAiringPublic to addAiringPublic2.  This allows the following mask to be set:
	LIVE_MASK and NEW_MASK
	audio: STEREO_MASK, SAP_MASK, DD51_MASK, DOLBY_MASK, DUBBED_MASK, SURROUND_MASK, SUBTITLE_MASK, CC_MASK, TAPE_MASK
	quality: HDTV_MASK, THREED_MASK, LETTERBOX_MASK, WIDESCREEN_MASK
	premiere: PREMIERE_MASK, CHANNEL_PREMIERE_MASK, SEASON_FINALE_MASK, SERIES_FINALE_MASK, SEASON_PREMIERE_MASK, SERIES_PREMIERE_MASK
	show part 
	total parts
##2.10 11/13/2022	
1.  Fixed <date> logic to use date in <previously-shown>
##2.11 11/13/2022	
1.  For Strings use isEmpty not isBlank
##3.0 9/16/2022

1.  Change `xmltv.error.log` to `xmltv_debug.log` for SageTV access info.
2.  Added a log file for each XMLTV configuration used: `xmltv_[config_name].log`.
3.  Changed `getLoggerPrinter()`, `log()`, `closeLoggers()` for multiple logger files: `xmltv_[config_name].log` -> `log()` and `xmltv_debug.log` -> `logDebug.log`.
4.  Changed `readConfiguration()`. Tries `xmltv.properties` and `*.xmltv.properties`. Now it will allow finding any config files without requiring `xmltv.properties`.
5.  Will download XMLTV file to the server directory with the following format: `xmltv_[provider_name].xml`. This was required because some XMLTV files were being locked due to still being open for writing.
6.  Added Actor to Sage guide.
7.  Fixed error where channel Icon is null.
8.  Added Character for show to Sage guide. Changed `addPersonToShow()`. Added processing of `<role>` tag and storing it in SageTV Character.
9.  Added parental Rating to Sage guide.
10. Added Show Icon URL to Sage guide. When the channel is selected in the guide, it will download the image from the URL.  Added `.properties` option 'sagetv.show.Icon'
11.  Added `.properties` option `programme.episode-num.system.showID_Value` for custom showID tag location.
12.  Added `.properties` option `sagetv.channel.NumberSeparator` for the channel separator used in SageTV.
13.  Added processing for `<series-id>` tag in `<programme>`. If no valid showId is found, it will use seriesId for showId.
14.  Read all custom channel info once from configuration and store it in its own channel list. This should speed up processing the guide as it does not need to read the `.properties` for each channel processed.
15.  Added filtering for `<team>` and `<sport>`. Not used as SageTV has no place to store it.
16.  Completely rewrote `addChannelToGuide()` for better processing of channels. Now it will process EPG123 xmltv files. It also processes external channel info provided in `.properties`. Also fixed how channel ID is calculated. It will use the first channel number for calculation. If no number is found, it will calculate it from the channel xmltvId by using CRC16_CCITT.
17.  Added miscellaneous `bit_mask` for SageTV show options in `addAiringPublic2` function.
18.  In `.properties` change option 'xmltv.channel.NumberOffset' to 'sagetv.channel.NumberOffset'
19.  In `.properties` change option 'xmltv.channel.IconDownload' to 'sagetv.channel.IconDownload'
19.  In `.properties` change option 'title.add.year' default to false
20.  In `.properties` change option 'episode.name.add.episode.number' default to false
21.  In `.properties` change option 'episode.name.add.part.number' default to false
22.  Add special case for parsing channelDVR  <programme><episode-num system=placeholder />/>  and <programme><episode-num system=xmltv: />/>
##3.0 10/26/2023
1.  Fixed Year date on movie
*/
package xmltv;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.io.BufferedInputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import sage.EPGDBPublic;
import sage.EPGDBPublic2;

/**
 * 
 * 
 * @Author: mta, jzhvymetal 
 */

 
public final class XMLTVImportPlugin implements sage.EPGImportPlugin,
        ContentHandler, ErrorHandler {

    //The newline sequence.
    private static final String ProgramVersion = "3.5";
	//The newline sequence.
    private static final String NEWLINE = System.getProperty("line.separator");
	//The date format used for logging. 
    private static final SimpleDateFormat DF_LOG = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss,SSS ");
	//The debug logging file.
    private static final File DEBUG_LOG_FILE = new File("xmltv_debug.log");
	//The logging file.
    private static File LOG_FILE = new File("xmltv.log");
	//The XMLTV logging file.
    private static File XMLTV_LOG_FILE= new File("xmltv.log");
	//The debug log writer.
    private static PrintStream sDebugLogPrinter;
	//The log writer. 
    private static PrintStream sLogPrinter;
	//The xmltv log writer. 
    private static PrintStream sXmltvLogPrinter;
	//The number of milliseconds that a log should be kept. (48 hours)
    private static final long LOG_TIMEOUT = 1000 * 60 * 60 * 48;
	private static final int DEFAULT_CHANNEL_ICON_MAX_WIDTH = 256;
	private static final int DEFAULT_CHANNEL_ICON_MAX_HEIGHT = 256;
	private static final int MAX_CHANNEL_ICON_DOWNLOAD_BYTES = 8 * 1024 * 1024;
	private static final long MAX_CHANNEL_ICON_SOURCE_PIXELS = 16L * 1024L * 1024L;
	private static final byte[] PNG_SIGNATURE = new byte[] {
		(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
	};
	private static long currentTimeMillis() {
		return Long.getLong("xmltv.test.currentTimeMillis", System.currentTimeMillis());
	}
	//Help field for translating collections to arrays.
    private static final String[] DUMMY_STRING_ARRAY = new String[0];
	//The default properties.
    private static final Properties DEFAULTS = createDefaults();
    private static final String DEFAULT_PROVIDER_ID = "867507149";
    //The regular expression for lowercase words <br>
    //(Words containing only lowercase unicode characters).
    private static final Pattern LOWERCASE_WORDS_PATTERN = Pattern.compile(" ([\\p{Ll}]+)");
	//The regular expression for an XMLTV numbering system number.
    private static final String RE_NS_PATTERN = "\\s*([0-9]+)?\\s*(?:/([0-9]+)?)?\\s*";
    //The regular expression for the XMLTV numbering system.
    private static final Pattern RE_EPISODE_NUMBER_PATTERN = Pattern
            .compile(RE_NS_PATTERN
                    + "\\."
                    + RE_NS_PATTERN
                    + "\\."
                    + RE_NS_PATTERN);
	//The ratings in order of restrictiveness.
    private static final List<String> RATINGS = Arrays.asList(new String[] {"AO",
            "TVM", "NC-17", "R", "TV14", "PG-13", "PG", "TVPG", "G", "TVG",
            "TVY7", "TVY", "NR"});
    //The advisory content strings.
    private static final List<String> ADVISORY_CONTENT_STRINGS = Arrays
            .asList(new String[] {"Graphic Violence", "Violence",
                    "Mild Violence", "Graphic Language", "Language",
                    "Adult Situations", "Strong Sexual Content", "Nudity",
                    "Brief Nudity", "Rape"});
	//All credit roles known to Sage.
    private static final List<String> ROLES = Arrays.asList(new String[] {null,
            "actor", "actor.lead", "actor.support", "actress", "actress.lead",
            "actress.support", "guest", "guest.star", "director", "producer",
            "writer", "choreographer", "sports.figure", "coach", "host",
            "producer.executive"});



	//The providers (map of provider-id's to a list of configurations).
    private static Map<String, List<Properties>> sProviders;
	private String XMLTV_Section="NONE";
	private String aQName_Start="";

	private SageGuideWriter guideWriter;
    //The current configuration.
    private Properties configuration;
	//The currently parsed channel.
    private Channel channel;
	//The currently parsed Character.
    private String character;
	//All parsed channels.
    private Map<String, Channel> channels;
	private Map<Integer, String> stationIdOwners;
	//The currently parsed show.
    private Show show;
	//The currently Init data from .propties file
	private Init init;
	//The currently parsed text. 
    private StringBuffer text;
	//The numbering system that is being parsed. 
    private String system;
    //Intermediate field for storing a value element.
	private String value;
    //The rating system.
	private String ratingSystem;
	private String elementLanguage;
	//Used to store current ProviderId being processed
	private String initProviderId;
	//Generated Show-ID strategy and persistent v2 mapping state.
	private ShowIdGenerator showIdGenerator;
	//Optional fail-open metadata consumer; owns no credentials, HTTP, or cache.
	private TmdbEnricher tmdbEnricher;
	private ImportResult importResult;
	private XmltvConfiguration typedConfiguration;
	
	private static boolean xLogDefaults=true;
	private static boolean xLogConfiguration=true;
	private static boolean xLogChannel=true;
	private static boolean xLogShow=true;
	
    /**
     * Creates the default properties.
     * 
     * @return the created default properties.
     */
    private static final Properties createDefaults() {
        Properties defaults = new Properties();
        defaults.put("configurations", "xmltv.properties");
        defaults.put("provider.name", "XMLTV Lineup");
		defaults.put("provider.id", "");
        defaults.put("xmltv.files", "epgdata.xml");
		defaults.put("xmltv.profile", "");
		defaults.put("xmltv.channel.display-name.ShortNameIndex", "0");
		defaults.put("xmltv.channel.display-name.ShortNameRegex", "");
		defaults.put("xmltv.channel.display-name.LongNameIndex", "0");
		defaults.put("sagetv.channel.NumberOffset", "0");
		defaults.put("sagetv.channel.NumberSeparator", ".");
		defaults.put("sagetv.show.Icon", "false");
		defaults.put("xmltv.channel.NumberTag", "");
		defaults.put("xmltv.channel.NumberTagIndex", "0");
		defaults.put("xmltv.channel.NumberTagRegEx", "");
		defaults.put("sagetv.channel.IconMaxWidth", Integer.toString(DEFAULT_CHANNEL_ICON_MAX_WIDTH));
		defaults.put("sagetv.channel.IconMaxHeight", Integer.toString(DEFAULT_CHANNEL_ICON_MAX_HEIGHT));
		defaults.put("xmltv.programme.episode-num.system.showID_Value", "");
		defaults.put("xmltv.show_id.strategy", ShowIdGenerator.LEGACY);
		defaults.put("xmltv.show_id.v2.map_file", ShowIdGenerator.DEFAULT_V2_MAP_FILE);
		defaults.put("xmltv.show_id.display", "none");
		defaults.put("xmltv.tmdb.enrich", "false");
		defaults.put("xmltv.tmdb.max_lookups_per_import", "250");
		defaults.put("xmltv.download.connect_timeout_ms", "10000");
		defaults.put("xmltv.download.read_timeout_ms", "30000");
		defaults.put("xmltv.download.max_bytes", "268435456");
		defaults.put("xmltv.download.max_redirects", "5");
		defaults.put("xmltv.validate_before_import", "true");
		defaults.put("xmltv.parser.max_element_chars", "4194304");
		defaults.put("xmltv.language.preferred", "");
		defaults.put("run.before.timeout_ms", "300000");
		defaults.put("run.before.fail_on_error", "true");
		defaults.put("run.before.log_command", "false");
		defaults.put("log.defaults", "true");
		defaults.put("log.configuration", "true");
		defaults.put("log.channel", "true");
		defaults.put("log.show", "true");
        defaults.put("inputstream.filter", "false");
        defaults.put("channel.ids", "*");
        defaults.put("initcap.title", "false");
        defaults.put("initcap.episode.name", "false");
        defaults.put("initcap.channel.ids", "*");
        defaults.put("initcap.skip.words", "the, a, an, at, in, on, and, "
                + "of, from, to, is, with, not, "
                + "en, de, der, het, op, voor, uit, van");
        defaults.put("title.add.year", "false");
        defaults.put("title.add.year.categories", "Movie");
        defaults.put("episode.name.add.episode.number", "false");
        defaults.put("episode.name.add.part.number", "false");
        defaults.put("split.movie.detect.time", "14400000");
        defaults.put("credits.director", "director");
        defaults.put("credits.actor", "actor");
        defaults.put("credits.writer", "writer");
        defaults.put("credits.producer", "producer");
        defaults.put("credits.presenter", "host");
        defaults.put("credits.guest", "guest");
        defaults.put("rating.*.NC-17", "NC-17");
        defaults.put("rating.*.X", "NC-17");
        defaults.put("rating.*.R", "R");
        defaults.put("rating.*.PG-13", "PG-13");
        defaults.put("rating.*.PG", "PG");
        defaults.put("rating.*.G", "G");
        defaults.put("rating.*.TV-MA", "TVM");
        defaults.put("rating.*.TV-MA-V", "TVM, Graphic Violence");
        defaults.put("rating.*.TV-MA-S", "TVM, Strong Sexual Content");
        defaults.put("rating.*.TV-MA-L", "TVM, Language");
        defaults.put("rating.*.TV-14", "TV14");
        defaults.put("rating.*.TV-14-V", "TV14, Violence");
        defaults.put("rating.*.TV-14-S", "TV14, Adult Situations");
        defaults.put("rating.*.TV-14-L", "TV14, Language");
        defaults.put("rating.*.TV-14-D", "TV14, Graphic Language");
        defaults.put("rating.*.TV-PG", "TVPG");
        defaults.put("rating.*.TV-PG-V", "TVPG, Mild Violence");
        defaults.put("rating.*.TV-PG-S", "TVPG, Adult Situations");
        defaults.put("rating.*.TV-PG-L", "TVPG, Language");
        defaults.put("rating.*.TV-PG-D", "TVPG, Graphic Language");
        defaults.put("rating.*.TV-G", "TVG");
        defaults.put("rating.*.TV-Y7", "TVY7");
        defaults.put("rating.*.TV-Y", "TVY");
        defaults.put("max.stars", "4");
        defaults.put("max.subcategories", Integer.toString(Integer.MAX_VALUE));
        defaults.put("report.missing.translate.category", "false");
        defaults.put("rerun.after.date", Integer.toString(Integer.MIN_VALUE));
        defaults.put("rerun.after.date.categories", "Series");
        defaults.put("hd.title.decoration", "HD - {0}");
        defaults.put("date.title.decoration", "{0} ({1,date,dd-MM-yyyy})");

        // Log the defaults.
        StringBuffer sb = new StringBuffer();
        TreeMap<String, String> map = new TreeMap<String, String>();
        Enumeration<?> names = defaults.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            map.put(name, defaults.getProperty(name));
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(NEWLINE + entry.getKey() + "=" + entry.getValue());
			
			
			
        }
        if(xLogDefaults)log("Defaults:" + sb);

        return defaults;
    }

    /**
     * Init for a configuration.
     * 
     * @param aConfiguration the configuration.
     */
    private final void initConfiguration(Properties aConfiguration) {
		init=new Init();
		this.typedConfiguration = new XmltvConfiguration(aConfiguration);
        // Log the defaults.
        StringBuffer sb = new StringBuffer();
        TreeMap<String, String> map = new TreeMap<String, String>();
        Enumeration<?> names = aConfiguration.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            map.put(name, aConfiguration.getProperty(name));
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(NEWLINE + entry.getKey() + "=" + entry.getValue());
			
			Pattern pattern;
			Matcher matcher;
			//Parse Init Channel Name
			pattern = Pattern.compile("channel\\.(.*?)\\.names");
			matcher = pattern.matcher(entry.getKey().toString());
			if (matcher.find())
			{
				String xmltvChannelId=matcher.group(1);
				//Check if channel already exist
				Channel InitChannel = this.init.channels.get(xmltvChannelId);
				//If null channel does not exist create new one
				if (InitChannel == null) InitChannel= new Channel(xmltvChannelId);
				//split all the name from .properties value
				String[] xmltvDisplayNames = entry.getValue().toString().split(",");
				//All all the names 
				for (int i = 0; i < xmltvDisplayNames.length; ++i) InitChannel.xmltvDisplayNames.add(xmltvDisplayNames[i]);
				//Put the channel back to the map of channels
				this.init.channels.put(xmltvChannelId, InitChannel);	
			}
			
			//Parse Init Channel network
			pattern = Pattern.compile("channel\\.(.*?)\\.network");
			matcher = pattern.matcher(entry.getKey().toString());
			if (matcher.find())	
			{
				String xmltvChannelId=matcher.group(1);
				//Check if channel already exist
				Channel InitChannel = this.init.channels.get(xmltvChannelId);
				//If null channel does not exist create new one
				if (InitChannel == null) InitChannel= new Channel(xmltvChannelId);
				//store network value
				InitChannel.network=entry.getValue().toString();
				//Put the channel back to the map of channels
				this.init.channels.put(xmltvChannelId, InitChannel);
			}
			
			//Parse Init Channel numbers
			pattern = Pattern.compile("channel\\.(.*?)\\.numbers");
			matcher = pattern.matcher(entry.getKey().toString());
			if (matcher.find())	
			{
				String xmltvChannelId=matcher.group(1);
				//Check if channel already exist
				Channel InitChannel = this.init.channels.get(xmltvChannelId);
				//If null channel does not exist create new one
				if (InitChannel == null) InitChannel= new Channel(xmltvChannelId);
				//split all the name from .properties value
				String[] xmltvDisplayNumbers = entry.getValue().toString().split(",");
				//All all the names 
				for (int i = 0; i < xmltvDisplayNumbers.length; ++i) InitChannel.numbers.add(xmltvDisplayNumbers[i]);
				//Put the channel back to the map of channels
				this.init.channels.put(xmltvChannelId, InitChannel);	
			}
			
        }
		xLogDefaults= isTrue(getProperty(aConfiguration, "log.defaults"),true);
		xLogConfiguration= isTrue(getProperty(aConfiguration, "log.configuration"),true);
	    xLogChannel=isTrue(getProperty(aConfiguration, "log.channel"),true);
		xLogShow=isTrue(getProperty(aConfiguration, "log.show"),true);
			
		if(xLogConfiguration)log("Configuration:" + this.typedConfiguration.redactedDump(NEWLINE));

        this.configuration = aConfiguration;
        this.init.ChannelShortNameIndex = this.typedConfiguration.getInt(
				"xmltv.channel.display-name.ShortNameIndex", 0, 0, 10000);
		this.init.ChannelShortNameRegEx = getProperty(aConfiguration, "xmltv.channel.display-name.ShortNameRegex");
		if (this.init.ChannelShortNameRegEx != null && this.init.ChannelShortNameRegEx.length() > 0) {
			this.init.ChannelShortNamePattern = Pattern.compile(this.init.ChannelShortNameRegEx);
		}
		this.init.ChannelLongNameIndex = this.typedConfiguration.getInt(
				"xmltv.channel.display-name.LongNameIndex", 0, 0, 10000);
		this.init.SagetvChannelNumberSeparator=getProperty(aConfiguration, "sagetv.channel.NumberSeparator");
		if(this.init.SagetvChannelNumberSeparator==null || !this.init.SagetvChannelNumberSeparator.equals("-"))
			this.init.SagetvChannelNumberSeparator=".";
		
		this.init.SagetvChannelNumberOffset = this.typedConfiguration.getInt(
				"sagetv.channel.NumberOffset", 0, -1000000, 1000000);
		
		this.init.SagetvShowIcon = isTrue(getProperty(aConfiguration, "sagetv.show.Icon"),false);
		
		this.init.ChannelNumberTag = getProperty(aConfiguration, "xmltv.channel.NumberTag");
		this.init.ChannelNumberTagIndex = this.typedConfiguration.getInt(
				"xmltv.channel.NumberTagIndex", 0, 0, 10000);
		
		this.init.ChannelNumberTagRegEx = getProperty(aConfiguration, "xmltv.channel.NumberTagRegEx");
		if (this.init.ChannelNumberTagRegEx != null && this.init.ChannelNumberTagRegEx.length() > 0) {
			this.init.ChannelNumberTagPattern = Pattern.compile(this.init.ChannelNumberTagRegEx);
		}
		
		this.init.SagetvChannelIconDownload = isChannelIconDownloadEnabled(aConfiguration);
		this.init.SagetvChannelIconMaxWidth = getBoundedPositiveInt(
				aConfiguration, "sagetv.channel.IconMaxWidth",
				DEFAULT_CHANNEL_ICON_MAX_WIDTH, 16, 2048);
		this.init.SagetvChannelIconMaxHeight = getBoundedPositiveInt(
				aConfiguration, "sagetv.channel.IconMaxHeight",
				DEFAULT_CHANNEL_ICON_MAX_HEIGHT, 16, 2048);
		
		
		this.init.ProgrammeEpisodeNumSystemShowID_Value = getProperty(aConfiguration, "xmltv.programme.episode-num.system.showID_Value");
		this.init.ShowIdDisplay = getProperty(aConfiguration, "xmltv.show_id.display");
		if (this.init.ShowIdDisplay == null) this.init.ShowIdDisplay = "none";
		this.init.ShowIdDisplay = this.init.ShowIdDisplay.trim().toLowerCase();
		if (!this.init.ShowIdDisplay.equals("none")
				&& !this.init.ShowIdDisplay.equals("description")
				&& !this.init.ShowIdDisplay.equals("bonus")) {
			throw new IllegalArgumentException("xmltv.show_id.display must be none, description, or bonus");
		}
		this.init.XmltvMaxElementChars = this.typedConfiguration.getInt(
				"xmltv.parser.max_element_chars", 4194304, 1024, 67108864);
		this.init.PreferredLanguage = this.typedConfiguration.get(
				"xmltv.language.preferred", "").toLowerCase(java.util.Locale.ROOT);
		
		
		this.initProviderId=getProperty(aConfiguration, "provider.id");
		if (this.initProviderId != null) {
			this.initProviderId = Long.toString(this.typedConfiguration.getLong(
					"provider.id", 0L, 1L, Long.MAX_VALUE));
		}


        this.init.channelIds = Arrays.asList(getStrings(getProperty(aConfiguration, "channel.ids")));

        this.init.capTitle = isTrue(getProperty(aConfiguration, "initcap.title"));
        this.init.capEpisodeName = isTrue(getProperty(aConfiguration, "initcap.episode.name"));
        this.init.capChannelIds = Arrays.asList(getStrings(getProperty(
                aConfiguration, "initcap.channel.ids")));
        this.init.capAllChannelIds = this.init.capChannelIds.contains("*");
        this.init.capSkipWords = Arrays.asList(getStrings(getProperty(
                aConfiguration, "initcap.skip.words")));

        this.init.titleAddYear = isTrue(getProperty(aConfiguration, "title.add.year"),false);
        this.init.titleAddYearCategories = Arrays.asList(getStrings(getProperty(
                aConfiguration, "title.add.year.categories")));

        this.init.episodeNameAddEpisodeNumber = isTrue(getProperty(aConfiguration,
                "episode.name.add.episode.number"),false);
        this.init.episodeNameAddPartNumber = isTrue(getProperty(aConfiguration,
                "episode.name.add.part.number"),false);

        this.init.directorRole = (byte) ROLES.indexOf(getProperty(aConfiguration,
                "credits.director"));
        this.init.actorRole = (byte) ROLES.indexOf(getProperty(aConfiguration,
                "credits.actor"));
        this.init.writerRole = (byte) ROLES.indexOf(getProperty(aConfiguration,
                "credits.writer"));
        this.init.adapterRole = (byte) ROLES.indexOf(getProperty(aConfiguration,
                "credits.adapter"));
        this.init.producerRole = (byte) ROLES.indexOf(getProperty(aConfiguration,
                "credits.producer"));
        this.init.presenterRole = (byte) ROLES.indexOf(getProperty(aConfiguration,
                "credits.presenter"));
        this.init.commentatorRole = (byte) ROLES.indexOf(getProperty(aConfiguration,
                "credits.commentator"));
        this.init.guestRole = (byte) ROLES.indexOf(getProperty(aConfiguration,
                "credits.guest"));

        this.init.splitMovieDetectTime = this.typedConfiguration.getLong(
				"split.movie.detect.time", 14400000L, 0L, 604800000L);
		String showIdMapFile = getProperty(aConfiguration, "xmltv.show_id.v2.map_file");
		if (showIdMapFile == null || showIdMapFile.trim().length() == 0) {
			showIdMapFile = ShowIdGenerator.DEFAULT_V2_MAP_FILE;
		}
		String configuredProviderId = getProperty(aConfiguration, "provider.id");
		String providerNamespace = configuredProviderId != null
				&& configuredProviderId.trim().length() > 0
				? "id:" + configuredProviderId.trim()
				: "name:" + getProperty(aConfiguration, "provider.name");
		try {
			this.showIdGenerator = new ShowIdGenerator(
					getProperty(aConfiguration, "xmltv.show_id.strategy"),
					providerNamespace, new File(showIdMapFile),
					this.init.splitMovieDetectTime);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to initialize XMLTV Show-ID mappings", e);
		}
		log("Show-ID strategy: " + this.showIdGenerator.getStrategy()
				+ (ShowIdGenerator.V2.equals(this.showIdGenerator.getStrategy())
						? "; mapping file: " + new File(showIdMapFile).getAbsolutePath() : ""));
		this.tmdbEnricher = TmdbEnricher.create(aConfiguration);
		log(this.tmdbEnricher.summary());

        this.init.maxStars = getIntProperty(aConfiguration, "max.stars");

        this.init.categoryTranslations = new HashMap<List<String>, List<String>>();
        this.init.maxCategoryTranslationLength = 0;
        names = aConfiguration.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            if (name.startsWith("translate.category.")) {
                String translation = aConfiguration.getProperty(name);
                List<String> from = Arrays.asList(name.substring(19).split(" */ *"));
                List<String> to = translation.length() > 0 ? Arrays.asList(translation
                        .split(" */ *")) : Collections.<String>emptyList();
                this.init.categoryTranslations.put(from, to);
                if (this.init.maxCategoryTranslationLength < from.size()) {
                    this.init.maxCategoryTranslationLength = from.size();
                }
            }
        }
        this.init.reportMissingTranslateCategory = isTrue(getProperty(
                aConfiguration, "report.missing.translate.category"));

        this.init.maxSubcategories = getIntProperty(aConfiguration,
                "max.subcategories");

        this.init.rerunAfterDate = getIntProperty(aConfiguration, "rerun.after.date");
        this.init.rerunAfterDateCategories = Arrays.asList(getStrings(getProperty(
                aConfiguration, "rerun.after.date.categories")));

        this.init.rerunNoEpisodeCategories = Arrays.asList(getStrings(getProperty(
                aConfiguration, "rerun.no-episode.categories")));
        this.init.rerunNoDateCategories = Arrays.asList(getStrings(getProperty(
                aConfiguration, "rerun.no-date.categories")));

        String decoration = getProperty(aConfiguration, "hd.title.decoration");
        if (decoration != null && decoration.length() > 0) {
            this.init.hdTitleDecoration = new MessageFormat(decoration);
            this.init.hdTitleDecorationChannels = Arrays
                    .asList(getStrings(getProperty(aConfiguration,
                            "hd.title.decoration.channels")));
        }

        decoration = getProperty(aConfiguration, "date.title.decoration");
        if (decoration != null && decoration.length() > 0) {
            this.init.dateTitleDecoration = new MessageFormat(decoration);
            this.init.dateTitleDecorationCategories = Arrays
                    .asList(getStrings(getProperty(aConfiguration,
                            "date.title.decoration.categories")));
        }

        this.init.categoriesForStarRating = Arrays.asList(getStrings(getProperty(
                aConfiguration, "categories.for.star-rating")));

        decoration = getProperty(aConfiguration, "date.year.decoration");
        if (decoration != null && decoration.length() > 0) {
            this.init.dateYearDecoration = new MessageFormat(decoration);
        }
    }

    /**
     * Exit for a configuration.
     */
    private final void exitConfiguration() {
		if (this.tmdbEnricher != null) {
			log(this.tmdbEnricher.summary());
			this.tmdbEnricher = null;
		}
		if (this.showIdGenerator != null) {
			try {
				this.showIdGenerator.flush();
			} catch (IOException e) {
				recordImportFailure("Show-ID mapping persistence", e);
				log("Unable to persist XMLTV v2 Show-ID mappings: " + e);
			}
			this.showIdGenerator = null;
		}
        // Resetting these objects prevents use of them out of context.
        // (also prevents a minor memory leak).
        this.configuration = null;
        this.init.channelIds = null;
        this.init.capChannelIds = null;
        this.init.capSkipWords = null;
        this.init.titleAddYearCategories = null;
        this.init.categoryTranslations = null;
        this.init.rerunAfterDateCategories = null;
        this.init.rerunNoEpisodeCategories = null;
        this.init.rerunNoDateCategories = null;
        this.init.hdTitleDecoration = null;
        this.init.hdTitleDecorationChannels = null;
        this.init.dateTitleDecoration = null;
        this.init.dateTitleDecorationCategories = null;
        this.init.categoriesForStarRating = null;
        this.init.dateYearDecoration = null;
		this.init=null;
		this.typedConfiguration = null;
    }

    /**
     * Init for a provider.
     */
    private final void initProvider() {
        this.channels = new HashMap<String, Channel>();
		this.stationIdOwners = new HashMap<Integer, String>();
    }

    /**
     * Exit for a provider.
     */
    private final void exitProvider() {
        // Resetting this object prevents use of it out of context.
        // (also prevents a minor memory leak).
        this.channels = null;
		this.stationIdOwners = null;
    }

    /**
     * Gets the configurations.
     * 
     * If the configuration files have changed they will be reread.
     * @return the list of configurations.
     */
    private synchronized static final Map<String, List<Properties>> getProviders() {
        // Provider files are small and setup/update calls are infrequent. A
        // fresh read also detects newly created, replaced, or deleted files.
        readConfigurations();
        return sProviders;
    }

    /**
     * Reads the configurations.
     */
    private synchronized static final void readConfigurations() {
        // Reset the entire configuration.
        sProviders = new TreeMap<String, List<Properties>>();
		Set<String> configurationFileNames = new LinkedHashSet<String>();
        Properties xmltvProperties = new Properties(DEFAULTS);
        try (FileInputStream inputStream = new FileInputStream("xmltv.properties")) {
            xmltvProperties.load(inputStream);
			configurationFileNames.addAll(Arrays.asList(
					getStrings(getProperty(xmltvProperties, "configurations"))));
        } catch (Exception e) {
            log("xmltv.properties not found processing *.xmltv.properties files");
        }
		
        try { //Process all *.xmltv.properties
			File dir = new File(".");
			File[] files = dir.listFiles(new FileFilter() {
					public boolean accept(File file) {
						return file.getName().toLowerCase().endsWith(".xmltv.properties");}});	
						
			if (files != null) {
				for (File file : files) configurationFileNames.add(file.getName());
			}
			
        } catch (Exception e) {
            log(e);
        }		
		        
		
		for (String configurationFileName : configurationFileNames) {
			File configurationFile = new File(configurationFileName);
			if (!configurationFile.isFile()) {
				log("Skipping missing XMLTV configuration: " + configurationFile.getAbsolutePath());
				continue;
			}
            Properties configuration = readConfiguration(configurationFile);
            String providerId = getProperty(configuration, "provider.id");
            if (providerId == null) {
                String providerName = getProperty(configuration,"provider.name");
                if ("XMLTV Lineup".equals(providerName)) {
                    providerId = "867507149";
                } else {
                    CRC32 crc32 = new CRC32();
                    try {
                        crc32.update(providerName.toLowerCase().getBytes(
                                "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        log(e);
                        crc32.update(providerName.getBytes());
                    }
                    providerId = Long.toString(crc32.getValue());
                }
			} else {
				try {
					long numericProviderId = Long.parseLong(providerId);
					if (numericProviderId <= 0) throw new NumberFormatException("not positive");
				} catch (NumberFormatException e) {
					log("Skipping XMLTV configuration with invalid provider.id: "
							+ configurationFile.getAbsolutePath());
					continue;
				}
            }
            List<Properties> configurations = sProviders.get(providerId);
            if (configurations == null) {
                configurations = new LinkedList<Properties>();
                sProviders.put(providerId, configurations);
				
            }
            configurations.add(configuration);
			
        }

        // Keep the plugin visible in SageTV before configuration. An installed
        // EPG import JAR is a selectable provider, not a reason to fall back to
        // the retired licensed SageTV EPG service. The placeholder uses the
        // documented defaults and can be configured later.
        if (sProviders.isEmpty()) {
            Properties defaultConfiguration = new Properties(DEFAULTS);
            List<Properties> configurations = new LinkedList<Properties>();
            configurations.add(defaultConfiguration);
            sProviders.put(DEFAULT_PROVIDER_ID, configurations);
            log("No XMLTV provider configuration found; exposing the default XMLTV Lineup provider.");
        }
    }

    /**
     * Reads a single configuration.
     * 
     * @param aConfigurationFile the configuration file to read.
     * @return the configuration.
     */
    private static final Properties readConfiguration(File aConfigurationFile) {
        try {
            return ConfigurationProfiles.load(aConfigurationFile, DEFAULTS,
                    new ConfigurationProfiles.LogSink() {
                        public void log(String message) {
                            XMLTVImportPlugin.log(message);
                        }
                    });
        } catch (Exception e) {
            // Profile support is additive. If profile resolution or automatic
            // generation fails, retain the historical direct-file behavior.
            log("Unable to resolve XMLTV profile for " + aConfigurationFile + ": " + e);
            Properties configuration = new Properties(DEFAULTS);
            try (FileInputStream inputStream = new FileInputStream(aConfigurationFile)) {
                configuration.load(inputStream);
            } catch (Exception fallbackError) {
                log(fallbackError);
            }
            return configuration;
        }
    }

    /**
     * Return a string array.
     * @param aCommaSeparatedList the list of comma separated values (the list 
     * is already assumed to be trimmed).
     * @return the string array.
     */
    private static final String[] getStrings(String aCommaSeparatedList) {
        if (aCommaSeparatedList != null && aCommaSeparatedList.length() > 0) {
            return aCommaSeparatedList.trim().split(" *, *");
        }
        return new String[0];
    }

    /**
     * Return <code>true</code> if a string denotes a true value.
     * 
     * @param aValue the string. 
	 * @param xNull Return boolean.	
     * @return true if a aValue denotes a true value..
     */
	private static final boolean isTrue(String aValue) {
		return isTrue(aValue, false);
	}
    /**
     * Return <code>true</code> if a string denotes a true value.
     * 
     * @param aValue the string. 
	 * @param NotFoundReturn boolean.	
     * @return true if a aValue denotes a true value..
     */
    private static final boolean isTrue(String aValue, boolean xNotFoundReturn ) {
        if (aValue == null) {
            return xNotFoundReturn;
        }
        String value = aValue.toLowerCase();
        return "true".equals(value) || "yes".equals(value) || "1".equals(value);
    }

	/**
	 * The original plugin used xmltv.channel.IconDownload and later releases
	 * renamed it. Keep both names so existing provider files continue to work;
	 * when both are present the documented sagetv.* name wins.
	 */
	static final boolean isChannelIconDownloadEnabled(Properties aProperties) {
		String value = getProperty(aProperties, "sagetv.channel.IconDownload");
		if (value == null) {
			value = getProperty(aProperties, "xmltv.channel.IconDownload");
		}
		return isTrue(value, false);
	}

	private static final int getBoundedPositiveInt(Properties aProperties,
			String aKey, int aDefault, int aMinimum, int aMaximum) {
		String value = getProperty(aProperties, aKey);
		if (value == null) {
			return aDefault;
		}
		try {
			int parsed = Integer.parseInt(value);
			if (parsed >= aMinimum && parsed <= aMaximum) {
				return parsed;
			}
		} catch (NumberFormatException e) {
			// The warning below includes the invalid value and the safe fallback.
		}
		log("Invalid " + aKey + "=" + value + "; using " + aDefault
				+ " (allowed range " + aMinimum + "-" + aMaximum + ")");
		return aDefault;
	}

    /**
     * Returns a property from properties.
     * 
     * @param aProperties the properties (or configuration) from which the 
     * value should be retrieved.
     * @param aKey the key of the property.
     * @return the value; <code>null</code> if the property is not present or
     * is empty. 
     */
    private static final String getProperty(Properties aProperties, String aKey) {
        String value = aProperties.getProperty(aKey);
        if (value != null) {
            value = value.trim();
            if (value.length() == 0) {
                return null;
            }
        }
        return value;
    }

	/**
	 * Download an XMLTV channel icon, validate the decoder input and always
	 * write a SageTV-compatible PNG. Images are downscaled to fit the requested
	 * bounds without changing their aspect ratio or upscaling small artwork.
	 */
	static final boolean downloadAndNormalizeChannelIcon(String aIconUrl,
			File aDestination, int aMaxWidth, int aMaxHeight) {
		if (aIconUrl == null || aIconUrl.trim().length() == 0
				|| aDestination == null || aMaxWidth < 1 || aMaxHeight < 1) {
			return false;
		}

		try {
			File parent = aDestination.getAbsoluteFile().getParentFile();
			if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
				throw new IOException("Unable to create channel logo directory: " + parent);
			}

			if (aDestination.isFile()) {
				try {
					BufferedImage existing = readBoundedImage(aDestination.toURI().toURL());
					if (existing.getWidth() <= aMaxWidth
							&& existing.getHeight() <= aMaxHeight
							&& hasPngSignature(aDestination)) {
						return true;
					}
					writePngAtomically(normalizeChannelIcon(existing, aMaxWidth, aMaxHeight),
							aDestination);
					log("Normalized existing channel logo " + aDestination.getName()
							+ " to " + imageDimensions(aDestination));
					return true;
				} catch (IOException e) {
					log("Existing channel logo is invalid; attempting replacement: "
							+ aDestination + " (" + e.getMessage() + ")");
				}
			}

			String iconUrl = aIconUrl.trim();
			if (iconUrl.startsWith("//")) {
				iconUrl = "https:" + iconUrl;
			}
			BufferedImage downloaded = readBoundedImage(new URL(iconUrl));
			BufferedImage normalized = normalizeChannelIcon(downloaded, aMaxWidth, aMaxHeight);
			writePngAtomically(normalized, aDestination);
			log("Downloaded channel logo " + aDestination.getName() + " from "
					+ iconUrl + " (" + downloaded.getWidth() + "x" + downloaded.getHeight()
					+ " -> " + normalized.getWidth() + "x" + normalized.getHeight() + " PNG)");
			return true;
		} catch (Exception e) {
			log("Channel logo download failed for " + aDestination + " from "
					+ aIconUrl + ": " + e);
			return false;
		}
	}

	private static final BufferedImage readBoundedImage(URL aUrl) throws IOException {
		URLConnection connection = aUrl.openConnection();
		connection.setUseCaches(false);
		connection.setConnectTimeout(10000);
		connection.setReadTimeout(20000);
		connection.setRequestProperty("Accept", "image/png,image/jpeg,image/gif,image/*;q=0.8");
		connection.setRequestProperty("User-Agent",
				"OpenSageTV-XMLTVImportPlugin/" + ProgramVersion);

		HttpURLConnection http = connection instanceof HttpURLConnection
				? (HttpURLConnection) connection : null;
		try {
			if (http != null) {
				http.setInstanceFollowRedirects(true);
				int status = http.getResponseCode();
				if (status < 200 || status >= 300) {
					throw new IOException("HTTP " + status);
				}
			}

			long contentLength = connection.getContentLengthLong();
			if (contentLength > MAX_CHANNEL_ICON_DOWNLOAD_BYTES) {
				throw new IOException("image payload exceeds "
						+ MAX_CHANNEL_ICON_DOWNLOAD_BYTES + " bytes");
			}

			byte[] data;
			try (InputStream input = new BufferedInputStream(connection.getInputStream());
					ByteArrayOutputStream output = new ByteArrayOutputStream()) {
				byte[] buffer = new byte[16384];
				int total = 0;
				int count;
				while ((count = input.read(buffer)) != -1) {
					total += count;
					if (total > MAX_CHANNEL_ICON_DOWNLOAD_BYTES) {
						throw new IOException("image payload exceeds "
								+ MAX_CHANNEL_ICON_DOWNLOAD_BYTES + " bytes");
					}
					output.write(buffer, 0, count);
				}
				data = output.toByteArray();
			}
			return decodeBoundedImage(data);
		} finally {
			if (http != null) {
				http.disconnect();
			}
		}
	}

	private static final BufferedImage decodeBoundedImage(byte[] aData) throws IOException {
		if (aData.length == 0) {
			throw new IOException("empty image payload");
		}
		try (ImageInputStream input = ImageIO.createImageInputStream(
				new ByteArrayInputStream(aData))) {
			if (input == null) {
				throw new IOException("unable to create image input stream");
			}
			Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if (!readers.hasNext()) {
				throw new IOException("unsupported or malformed image format");
			}
			ImageReader reader = readers.next();
			try {
				reader.setInput(input, true, true);
				int width = reader.getWidth(0);
				int height = reader.getHeight(0);
				if (width < 1 || height < 1
						|| (long) width * (long) height > MAX_CHANNEL_ICON_SOURCE_PIXELS) {
					throw new IOException("unsafe source dimensions " + width + "x" + height);
				}
				BufferedImage image = reader.read(0);
				if (image == null) {
					throw new IOException("image decoder returned no pixels");
				}
				return image;
			} finally {
				reader.dispose();
			}
		}
	}

	private static final BufferedImage normalizeChannelIcon(BufferedImage aSource,
			int aMaxWidth, int aMaxHeight) {
		double scale = Math.min(1.0d, Math.min(
				(double) aMaxWidth / (double) aSource.getWidth(),
				(double) aMaxHeight / (double) aSource.getHeight()));
		int width = Math.max(1, (int) Math.round(aSource.getWidth() * scale));
		int height = Math.max(1, (int) Math.round(aSource.getHeight() * scale));
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = result.createGraphics();
		try {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
					RenderingHints.VALUE_RENDER_QUALITY);
			graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
					RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			graphics.drawImage(aSource, 0, 0, width, height, null);
		} finally {
			graphics.dispose();
		}
		return result;
	}

	private static final void writePngAtomically(BufferedImage aImage, File aDestination)
			throws IOException {
		File parent = aDestination.getAbsoluteFile().getParentFile();
		File temporary = File.createTempFile(".xmltv-logo-", ".png", parent);
		try {
			if (!ImageIO.write(aImage, "png", temporary)) {
				throw new IOException("PNG encoder is unavailable");
			}
			try {
				Files.move(temporary.toPath(), aDestination.toPath(),
						StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temporary.toPath(), aDestination.toPath(),
						StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			if (temporary.exists() && !temporary.delete()) {
				temporary.deleteOnExit();
			}
		}
	}

	private static final boolean hasPngSignature(File aFile) throws IOException {
		try (InputStream input = new FileInputStream(aFile)) {
			for (int i = 0; i < PNG_SIGNATURE.length; i++) {
				if (input.read() != (PNG_SIGNATURE[i] & 0xff)) {
					return false;
				}
			}
			return true;
		}
	}

	private static final String imageDimensions(File aFile) {
		try {
			BufferedImage image = ImageIO.read(aFile);
			return image == null ? "unknown dimensions" : image.getWidth() + "x" + image.getHeight() + " PNG";
		} catch (IOException e) {
			return "unknown dimensions";
		}
	}

    static final void logXMLTV(Object aObject) {
		sXmltvLogPrinter=getLogPrinter(XMLTV_LOG_FILE, sXmltvLogPrinter);
		logger(aObject, sXmltvLogPrinter);
	}

    static final void log(Object aObject) {
		sLogPrinter=getLogPrinter(LOG_FILE, sLogPrinter);
		logger(aObject, sLogPrinter);
	}
	
	static final void logDebug(Object aObject) {
		sDebugLogPrinter=getLogPrinter(DEBUG_LOG_FILE, sDebugLogPrinter);
		logger(aObject, sDebugLogPrinter);
	}

    static synchronized final void logger(Object aObject, PrintStream aLog) {
        if (aObject == null) {
            aLog.println("null");
		} else if (aObject instanceof String) {
			String aMessage=String.class.cast(aObject);
			aLog.println(DF_LOG.format(new Date()) + aMessage);
        } else if (aObject instanceof Throwable) {
			Throwable aException=Throwable.class.cast(aObject);
			aLog.print(DF_LOG.format(new Date()));
			aException.printStackTrace(aLog);
			// Log in the normal log file as well.
			aLog.println(DF_LOG.format(new Date()) + aException);
		} else if (aObject instanceof InputStream) {
			InputStream aInputStream=InputStream.class.cast(aObject);
			byte[] buffer = new byte[1000];
			try {
				int bytesRead = aInputStream.read(buffer);
				while (bytesRead > 0) {
					aLog.print(new String(buffer, 0, bytesRead));
					bytesRead = aInputStream.read(buffer);
				}
			} catch (IOException e) {
				logger(e, aLog);
			}
        } else {
            logger(aObject.toString(), aLog);
        }
		aLog.flush();
    }

    /**
     * Gets an active log printer.
     * 
     * @return the log printer.
     */
    private static final PrintStream getLogPrinter(File aLog, PrintStream aLogPrinter) {
		try 
		{
			long diff = new Date().getTime() - aLog.lastModified();
			if (diff > LOG_TIMEOUT) aLog.delete();
			
			if (aLogPrinter == null) 
				aLogPrinter = new PrintStream(new FileOutputStream(aLog, true));
			
		}catch (Exception e) {
			e.printStackTrace(System.err);
			return System.err;
		}
		return aLogPrinter;
	}


    /**
     * Close the log.
     */
    private synchronized static final void closeLoggers() {
        if (sLogPrinter != null) {
            if (sLogPrinter != System.out && sLogPrinter != System.err) {
                sLogPrinter.close();
            }
            sLogPrinter = null;
        }
		
		if (sDebugLogPrinter != null) {
            if (sDebugLogPrinter != System.out && sDebugLogPrinter != System.err) {
                sDebugLogPrinter.close();
            }
            sDebugLogPrinter = null;
        }
		
		if (sXmltvLogPrinter != null) {
            if (sXmltvLogPrinter != System.out && sXmltvLogPrinter != System.err) {
                sXmltvLogPrinter.close();
            }
            sXmltvLogPrinter = null;
        }
    }

	/*
	* SageTV implements EPGImportPlugin.java
	* Returns a String[][2]. Each element pair represents a provider ID & a provider name.
	* These names will be displayed in the Setup Wizard for SageTV if Local Markets are selected
	* for the provider. The provider GUIDs must be valid arguments
	* for Long.parseLong(providerID), represent positive numbers, and be consistent and unique for given provider.
	* An example of the return value is:
	* { { "3", "Test Local Lineup1"} {"4", "Test Local Lineup2"} }
	*
	* public String[][] getLocalMarkets();
	*/
    public final String[][] getLocalMarkets() {
        try { 
			String[][] ProviderArray=getProviderArray();
			logXMLTV("Version: " + this.ProgramVersion);
			logXMLTV("getLocalMarkets()");
            return ProviderArray;

        } catch (Throwable t) {
            logXMLTV(t);
            if (t instanceof Error) throw (Error) t;
            throw new RuntimeException("Unable to enumerate XMLTV local markets", t);
        } finally {
            cleanup();
        }
    }

    /**
     * Gets the array of providers.
     * 
     * @return the array.
     */
    private static final String[][] getProviderArray() {
        Map<String, List<Properties>> providers = getProviders();
        String[][] localMarkets = new String[providers.size()][2];
        int i = 0;
        for (Map.Entry<String, List<Properties>> entry : providers.entrySet()) {
            localMarkets[i][0] = entry.getKey();
            List<Properties> configurations = entry.getValue();
            Properties configuration = configurations.get(0);
            localMarkets[i][1] = getProperty(configuration, "provider.name");
            ++i;
        }
        return localMarkets;
    }

    /**
     * Gets an integer property.
     * @param aProperties the properties (or configuration) from which the 
     * value should be retrieved.
     * @param aKey the key of the property.
     * @return the value 
     */
    private final int getIntProperty(Properties aProperties, String aKey) {
        String string = getProperty(aProperties, aKey);
        if (string == null) {
            // The user has provided an empty value.
            string = getProperty(DEFAULTS, aKey);
        }
        if (string == null) {
            // The programmer has not provided a default value.
            return 0;
        }
        return Integer.parseInt(string);
    }

    /**
     * Exit the plugin.
     */
    private final void cleanup() {
        closeLoggers();
    }

	/*
	* SageTV implements EPGImportPlugin.java
	* Returns a String[][2] for a given zip code. Each element pair represents a provider GUID & a provider name.
	* These names will be displayed in the Setup Wizard for SageTV. The provider GUIDs must be valid arguments
	* for Long.parseLong(providerID), represent positive numbers, and be consistent and unique for given provider.
	* An example of the return value is:
	* { { "1", "Test Lineup1"} {"2", "Test Lineup2"} }
	*public String[][] getProviders(String zipCode);
	*/
    public final String[][] getProviders(String aZipCode) {
        try {
			String[][] ProviderArray=getProviderArray();
			logXMLTV("Version: " + this.ProgramVersion);
            logXMLTV("getProviders(" + aZipCode + ")");
            // Ignore the zipcode
            return ProviderArray;

        } catch (Throwable t) {
            logXMLTV(t);
            if (t instanceof Error) throw (Error) t;
            throw new RuntimeException("Unable to enumerate XMLTV providers", t);
        } finally {
            cleanup();
        }
    }

	/**
	* SageTV implements EPGImportPlugin.java
	* This is called daily (or more often if other events occur) to update the EPG database.
	* It should return true if successful, and false if not. The providerID argument
	* references the provider selected from the functions above. The EPGDBPublic object
	* is what is used to make calls to the database to provide it with the new EPG information.
	* See the documentation on EPGDBPublic for more information
	* 
	* public boolean updateGuide(String providerID, EPGDBPublic dbInterface);
	*/
    public final boolean updateGuide(String aProviderId, EPGDBPublic aGuide) {
        ImportResult result = new ImportResult();
        this.importResult = result;
        boolean providerInitialized = false;
        try {
			File TEMP_LOG_FILE = new File("xmltv_" + aProviderId +"_temp.log");
			LOG_FILE=TEMP_LOG_FILE;
			log("Version: " + this.ProgramVersion);
            log("updateGuide(" + aProviderId + ", " + aGuide + ")");
			logXMLTV("Version: " + this.ProgramVersion);
			logXMLTV("updateGuide(" + aProviderId + ", " + aGuide + ")");
			
			if (aProviderId == null || aGuide == null) {
				result.fail("provider", "provider ID and guide database are required");
				return false;
			}
			if (!(aGuide instanceof EPGDBPublic2)) {
				result.fail("provider", "SageTV guide database does not implement EPGDBPublic2");
				return false;
			}
			Long.parseLong(aProviderId);
			initProvider();
			providerInitialized = true;

			this.guideWriter = new SageGuideWriter((EPGDBPublic2) aGuide);
			String providerName="";
			List<Properties> configurations = getConfigurations(aProviderId);
			if (!configurations.isEmpty()) {
				if (configurations.size() > 0) {
					providerName = getProperty(configurations.get(0),"provider.name");
					log("Provider name = " + providerName);
					logXMLTV("Provider name = " + providerName);
					logXMLTV("Start guide update for " + providerName);
					//Close Loggers to use new LOG_FILE;
					closeLoggers();
					//Create New logger file with providerName in the name
					if(providerName!=null)
					{
						LOG_FILE = new File("xmltv_" + providerName + ".log");
						//Copy all the data from temp log before the providerName was know
						copyFileContentAppend(TEMP_LOG_FILE,LOG_FILE);
					}
					else
					{
						LOG_FILE = new File("xmltv.log");
					}
					//Delete the old temp log file
					TEMP_LOG_FILE.delete();
					int i = 0;
					for (Properties config : configurations) {
						result.configurationStarted();
						int failuresBefore = result.getFailureCount();
						try {
							log("Configuration "+ providerName+ "["+ i++ + "]");
							if (updateConfiguration(config)
									&& result.getFailureCount() == failuresBefore) {
								result.configurationSucceeded();
							}
						} catch (Exception e) {
							log(e);
							result.fail("configuration " + (i - 1), e);
						}
					}
				}

				TreeMap<Integer, String[]> lineup = new TreeMap<Integer, String[]>();
				int nextChannelNumber = 2;
				for (Channel availableChannel : this.channels.values()) {
					this.channel = availableChannel;
					if (this.channel.numbers.size() == 0) {
						lineup.put(this.channel.STVstationID, new String[] {Integer.toString(nextChannelNumber++)});
					} else {
						lineup.put(this.channel.STVstationID, this.channel.numbers.toArray(new String[0]));
					}
					this.channel = null;
				}
				  /*
				   * Call this with the current providerID, and a map of stationIDs to channel numbers. The keys in the map
				   * should be Integer objects wrapping stationIDs as used in the addChannelPublic method. The values in the map
				   * should be String[] that represent the channel numbers for that station. An example is if ESPN w/ stationdID=34
				   * is on channel numbers 3 and 94, the map would contain a: Integer(34)->{"3", "94"}
					public void setLineup(long providerID, java.util.Map lineupMap);
				   */
				if (!result.hasFailures()) {
					this.guideWriter.setLineup(Long.parseLong(aProviderId), lineup);
					logXMLTV("Finish guide update for " + providerName);
				} else {
					logXMLTV("Guide update failed; existing lineup was not replaced for " + providerName);
				}
			}
			else
			{
				result.fail("provider", "no configuration found for provider " + aProviderId);
				logXMLTV("No configuration found for provider " + aProviderId);
			}


        } catch (Error error) {
			log(error);
			throw error;
		} catch (Exception exception) {
			log(exception);
			result.fail("provider update", exception);
        } finally {
			log("Import summary: " + result);
			for (String failure : result.getFailures()) log("Import failure: " + failure);
            cleanup();
			if (providerInitialized) exitProvider();
			this.guideWriter = null;
			this.importResult = null;
        }
		return result.isSuccessful();
		
    }

    /**
     * Gets the configuration for a specific provider.
     * 
     * @param aProviderId the provider.
     * @return the list of configurations for that provider.
     */
    private static synchronized final List<Properties> getConfigurations(String aProviderId) {
        List<Properties> configurations = null;
        if (aProviderId != null) {
            Map<String, List<Properties>> providers = getProviders();
            if (providers != null) {
                configurations = providers.get(aProviderId);
            }
        }
        return configurations == null ? Collections.<Properties>emptyList() : configurations;
    }

    /**
     * Update the guide for a specific configuration.
     * 
     * @param aConfiguration the configuration that is used for the update.
     */
    private final boolean updateConfiguration(Properties aConfiguration) {
        boolean initialized = false;
        boolean successful = true;
        try {
			initConfiguration(aConfiguration);
			initialized = true;
            waitForTimeslot(aConfiguration);
			if (!executeRunBefore(aConfiguration)) return false;
            String[] xmltvFiles = getStrings(getProperty(aConfiguration,"xmltv.files"));
            boolean inputStreamFilter = isTrue(getProperty(aConfiguration, "inputstream.filter"));
			if (xmltvFiles.length == 0) {
				this.importResult.fail("feed", "no xmltv.files sources are configured");
				return false;
			}
			FeedDownloader.Options options = new FeedDownloader.Options(
					this.typedConfiguration.getInt("xmltv.download.connect_timeout_ms", 10000, 100, 600000),
					this.typedConfiguration.getInt("xmltv.download.read_timeout_ms", 30000, 100, 600000),
					this.typedConfiguration.getLong("xmltv.download.max_bytes", 268435456L,
							1024L, 2147483648L),
					this.typedConfiguration.getInt("xmltv.download.max_redirects", 5, 0, 20));
			String providerFileName = safeFileComponent(getProperty(aConfiguration, "provider.name"));
            for (int i = 0; i < xmltvFiles.length; ++i) {
				this.importResult.feedStarted();
				String cacheName = "xmltv_" + providerFileName
						+ (i == 0 ? "" : "_" + (i + 1)) + ".xml";
                try {
					log("Acquiring " + XmltvConfiguration.redactUri(xmltvFiles[i])
							+ " to " + cacheName);
					File cached = FeedDownloader.acquire(xmltvFiles[i], new File(cacheName), options);
					this.show = null;
					this.channel = null;
					this.text = null;
					log("Parsing: " + cached.getName());
					XmltvParser.parse(cached, inputStreamFilter,
							this.typedConfiguration.getBoolean("xmltv.validate_before_import", true),
							this, this);
					this.importResult.feedSucceeded();
                } catch (Exception exception) {
					successful = false;
					this.importResult.fail("feed " + (i + 1), exception);
					log(exception);
                }
            }

                for (Iterator<String> it = this.init.channelIds.iterator(); it.hasNext();) {
                    // Add missing channels.
                    String xmltvChannelId = it.next();
                    if (!this.channels.containsKey(xmltvChannelId) && !xmltvChannelId.equals("*")) {
                        this.channel = new Channel(xmltvChannelId);
                        addChannelToGuide();
                    }
                }
                this.channel = null;
			return successful;
		} catch (Error error) {
			throw error;
		} catch (Exception exception) {
			this.importResult.fail("configuration", exception);
			log(exception);
			return false;
        } finally {
			if (initialized || this.init != null) exitConfiguration();
        }
    }

	private static String safeFileComponent(String value) {
		String safe = value == null ? "provider" : value.replaceAll("[^A-Za-z0-9._-]", "_");
		while (safe.contains("..")) safe = safe.replace("..", "_");
		if (safe.length() == 0) safe = "provider";
		return safe.length() > 80 ? safe.substring(0, 80) : safe;
	}



    /**
     * Lets the current thread wait for a specific timeslot before starting.
     * 
     * @param aConfiguration the configuration.
     */
    private void waitForTimeslot(Properties aConfiguration) {
        String timeslot = aConfiguration.getProperty("timeslot");
        if (timeslot == null) {
            return;
        }
        if (timeslot.length() < 9) {
            log("Invalid timeslot: " + timeslot);
            log("timeslot should adhere to the format: hhmm-hhmm");
            return;
        }

        log("Checking timeslot");
        Calendar now = Calendar.getInstance();
        Calendar from = (Calendar) now.clone();
        from.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeslot
                .substring(0, 2)));
        from.set(Calendar.MINUTE, Integer.parseInt(timeslot.substring(2, 4)));
        from.set(Calendar.SECOND, 0);
        from.set(Calendar.MILLISECOND, 0);
        Calendar until = (Calendar) now.clone();
        until.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeslot.substring(5, 7)));
        until.set(Calendar.MINUTE, Integer.parseInt(timeslot.substring(7, 9)));
        until.set(Calendar.SECOND, 0);
        until.set(Calendar.MILLISECOND, 0);
        if (from.after(until)) {
            from.add(Calendar.DATE, -1);
        }

        while (now.after(until)) {
            from.add(Calendar.DATE, 1);
            until.add(Calendar.DATE, 1);
        }
        log(" from  = " + from.getTime());
        log(" until = " + until.getTime());

        if (now.after(from)) {
            log("We're in the correct timeslot.");
            return;
        }

        log("Waiting for " + from.getTime());
        try {
            Thread.sleep(from.getTimeInMillis() - now.getTimeInMillis());
        } catch (InterruptedException e) {
			Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for XMLTV import timeslot", e);
        }
        log("Waited for timeslot since " + now.getTime());
        log("Continuing update.");
    }



    /**
     * Run the program that has been configured to run before parseing.
     * 
     * @param aConfiguration the configuration.
     */
    private final boolean executeRunBefore(Properties aConfiguration) {
        try {
            String runBefore = getProperty(aConfiguration, "run.before");
			if (runBefore == null || runBefore.trim().length() == 0) return true;
			if (this.typedConfiguration.getBoolean("run.before.log_command", false)) {
				log("Executing run.before: " + runBefore);
			} else {
				log("Executing configured run.before command (command text redacted)");
			}
			long timeout = this.typedConfiguration.getLong("run.before.timeout_ms",
					300000L, 100L, 3600000L);
			ExternalCommandRunner.Result result = ExternalCommandRunner.run(runBefore,
					timeout, new ExternalCommandRunner.LogSink() {
						public void log(String line) {
							XMLTVImportPlugin.log("run.before: " + line);
						}
					});
			if (result.isSuccessful()) {
				log("run.before completed successfully");
				return true;
			}
			String message = result.timedOut ? "run.before timed out"
					: "run.before exited with status " + result.exitCode;
			log(message);
			if (this.typedConfiguration.getBoolean("run.before.fail_on_error", true)) {
				this.importResult.fail("run.before", message);
				return false;
			}
			return true;
        } catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			this.importResult.fail("run.before", e);
			log(e);
			return false;
        } catch (Exception e) {
			this.importResult.fail("run.before", e);
			log(e);
			return false;
        }
    }

    /* XML Parsing */

    public final void characters(char[] aCh, int aStart, int aLength)
            throws SAXException {
        if (this.text != null) {
			if (this.text.length() + aLength > this.init.XmltvMaxElementChars) {
				throw new SAXException("XMLTV element text exceeds configured limit");
			}
            this.text.append(aCh, aStart, aLength);
        }
    }

    public final void startDocument() throws SAXException {
    // noop
    }

    public final void endDocument() throws SAXException {
    // noop
    }

    public final void startElement(String aNamespaceURI, String aLocalName,
            String aQName, Attributes aAttributes) throws SAXException {	
		aQName_Start=aQName;
		this.elementLanguage = aAttributes.getValue("lang");
		this.text = new StringBuffer();  //New String buffer to parse and text values
        if (aQName.equals("channel")) {	
			XMLTV_Section="channel";
            String xmltvChannelId = aAttributes.getValue("id");
			//Check if channel alreay exist if not create new channel
            this.channel = (Channel) this.channels.get(xmltvChannelId);
            if (this.channel == null) {
                this.channel = new Channel(xmltvChannelId);
            }
			
        } else if (aQName.equals("programme")) {
			XMLTV_Section="programme";
            this.show = new Show(parseXmltvDate(aAttributes.getValue("start")),
                    parseXmltvDate(aAttributes.getValue("stop")));
            String xmltvChannelId = aAttributes.getValue("channel");
            if (xmltvChannelId != null && (this.init.channelIds.contains("*") || this.init.channelIds.contains(xmltvChannelId))) {
                this.channel = (Channel) this.channels.get(xmltvChannelId);
                if (this.channel == null) {
                    //never seen before channel, create it
                    this.channel = new Channel(xmltvChannelId);
					//FIXME cannot add channel to guide since not all infomation gattered
                    addChannelToGuide();
                }
            }
        } else if (aQName.equals("episode-num")) {
            this.system = aAttributes.getValue("system");
		 } else if (aQName.equals("series-id")) {
            this.system = aAttributes.getValue("system");
        } else if ((aQName.equals("title")
                || aQName.equals("sub-title")
                || aQName.equals("desc")
                || aQName.equals("director")
                || aQName.equals("writer")
                || aQName.equals("adapter")
                || aQName.equals("producer")
                || aQName.equals("presenter")
                || aQName.equals("commentator")
                || aQName.equals("guest")
                || aQName.equals("date")
                || aQName.equals("category")
                || aQName.equals("language")
                || aQName.equals("orig-language")
                || aQName.equals("country")
                || aQName.equals("colour")
                || aQName.equals("aspect")
                || aQName.equals("quality")
                || aQName.equals("stereo") || aQName.equals("value"))
                ) {
				//NOP	
		} else if (aQName.equals("actor")) {
			    this.character= aAttributes.getValue("role");
				if(this.character==null)this.character="Actor";
        } else if (aQName.equals("display-name")) {
		} else  if(aQName.equals("icon")){
				String xmltvIcon=aAttributes.getValue("src");
				if(xmltvIcon!=null)
				{
					if (XMLTV_Section.equals("channel") && this.channel != null)
						this.channel.xmltvIcon = xmltvIcon;
						
					if (XMLTV_Section.equals("programme") && this.show != null)
						this.show.xmltvIcon = xmltvIcon;	
				}
        } else if (aQName.equals("previously-shown")) {
            this.show.is_rerun = true;
			String previousStart = aAttributes.getValue("start");
			if (previousStart != null && previousStart.trim().length() > 0) {
				this.show.previous_start = parseXmltvDate(previousStart);
			}
        } else if (aQName.equals("premiere")) {
            this.show.is_premiere = true;
        } else if (aQName.equals("new")) {
            // The first showing of the first episode of a new show?
            // Sounds like a premiere to me...
            this.show.is_premiere = true;
			this.show.is_new = true;
		} else if (aQName.equals("live")) {
            // The first showing of the first episode of a new show?
            // Sounds like a premiere to me...
            this.show.is_premiere = true;
			this.show.is_new = true;
			this.show.is_live = true;
        } else if (aQName.equals("subtitles")) {
            this.show.subtitles = aAttributes.getValue("type");
            if (this.show.subtitles == null) {
                this.show.subtitles = "yes";
            }
        } else if (aQName.equals("rating")) {
            this.ratingSystem = aAttributes.getValue("system");
            if (this.ratingSystem == null) {
                this.ratingSystem = "";
            }
        } else if (aQName.equals("star-rating")) {
            // noop
		} else if (this.init.ChannelNumberTag!=null && aQName.equals(this.init.ChannelNumberTag)) {
            // Custom XMLTV tag to get channel number
        } else {
			if (!ignoreQName(aQName)) {
				String logText="<" + aQName + "> Not being parsed";
				logDebug(logText);
				log(logText);
			}
        }
    }

    /**
     * Tests if a QName should be ignored.
     * 
     * @param aQName the QName that should be examined.
     * @return <code>true</code> if the QName should be ignored.
     */
    private static final boolean ignoreQName(String aQName) {
        return aQName.equals("tv")
                || aQName.equals("url")
                || aQName.equals("credits")
                || aQName.equals("length")
                || aQName.equals("video")
                || aQName.equals("present")
                || aQName.equals("audio")
				|| aQName.equals("sport")
				|| aQName.equals("team")
                || aQName.equals("last-chance");
    }

    public final void endElement(String aNamespaceURI, String aLocalName, String aQName) throws SAXException {

		try{		

		if (aQName.equals("channel")) {
			if ((this.init.channelIds.contains("*") || this.init.channelIds.contains(this.channel.xmltvId))
					&& !this.channels.containsKey(this.channel.xmltvId) ) {		
				addChannelToGuide();
			}
			this.channel = null;
		} else if (aQName.equals("programme")) {//Done Parsing Show 
			if(this.show.date == null && this.show.year==null)  //Check if there is vaild date 
			{
				if(this.show.previous_start!= null)	//Use previous start if no date set
					this.show.date=this.show.previous_start;
				else 
					if(!this.show.is_rerun || this.show.is_live || this.show.is_new)
						this.show.date=this.show.start; //If airdate is null it means it is new or live
			}
			
			if(this.show.date != null && this.show.year==null )
			{
				this.show.year = XmltvDateParser.year(this.show.date);
			}

			addShowToGuide();  //Data is ready to add to the guide
			this.show = null;
			this.channel = null;
		} else if (XMLTV_Section.equals("channel") && aQName.equals("display-name") && this.channel != null) {
			this.channel.xmltvDisplayNames.add(this.text.toString());
		} else if (aQName.equals("title")) {
			if (shouldUseLanguage(this.show.titleLanguage, this.elementLanguage)) {
				this.show.title = this.text.toString();
				this.show.titleLanguage = this.elementLanguage;
			}
		} else if (aQName.equals("sub-title")) {
			if (shouldUseLanguage(this.show.episodeNameLanguage, this.elementLanguage)) {
				this.show.episodeName = this.text.toString();
				this.show.episodeNameLanguage = this.elementLanguage;
			}
		} else if (aQName.equals("desc")){ 
			if (!XMLTV_Section.equals("channel")) {
				String description = this.text.toString();
				if (isPreferredLanguage(this.elementLanguage)) {
					this.show.descriptions.add(0, description);
					this.show.descriptionLanguages.add(0, this.elementLanguage);
				} else {
					this.show.descriptions.add(description);
					this.show.descriptionLanguages.add(this.elementLanguage);
				}
			}
		} else if (aQName.equals("director")) {
			addPersonToShow(this.text.toString(), this.init.directorRole, "Director");
		} else if (aQName.equals("actor")) {
			addPersonToShow(this.text.toString(), this.init.actorRole, this.character);
		} else if (aQName.equals("writer")) {
			addPersonToShow(this.text.toString(), this.init.writerRole, "Writer");
		} else if (aQName.equals("adapter")) {
			addPersonToShow(this.text.toString(), this.init.adapterRole, "Adapter");
		} else if (aQName.equals("producer")) {
			addPersonToShow(this.text.toString(), this.init.producerRole, "Producer");
		} else if (aQName.equals("presenter")) {
			addPersonToShow(this.text.toString(), this.init.presenterRole, "Presenter" );
		} else if (aQName.equals("commentator")) {
			addPersonToShow(this.text.toString(), this.init.commentatorRole,"Commentator");
		} else if (aQName.equals("guest")) {
			addPersonToShow(this.text.toString(), this.init.guestRole, "Guest");
		} else if (aQName.equals("date")) {
			String date = this.text.toString();
			try {
				if (date.length() >= 8) {
					this.show.date = XmltvDateParser.parseDay(date);
				}
				
				if (date.length() == 4) {
					this.show.year = date;						
				}
			} catch (IllegalArgumentException e) {
				this.show.date=null;
				log(e);
			}
		   
		} else if (aQName.equals("category")) {
			String[] categories = this.text.toString().split("/");
			for (int i = 0; i < categories.length; ++i) {
				this.show.categories.add(categories[i]);
			}
		} else if (aQName.equals("language")) {
			if (this.show.subtitles == null) {
				// Subtitles seem to have a language element as well.
				this.show.language = this.text.toString();
			}
		} else if (aQName.equals("orig-language")) {
			this.show.originalLanguage = this.text.toString();
		} else if (aQName.equals("country")) {
			this.show.countries.add(this.text.toString());
		} else if (aQName.equals("episode-num")) {
			parseEpisodeNumber(this.text.toString());

		} else if (aQName.equals("series-id")) {
			this.show.seriesId=this.text.toString();
		} else if (aQName.equals("previously-shown")) {
			String date=this.text.toString();
			if(date!=null && !date.isEmpty())this.show.previous_start=parseXmltvDate(date);
		} else if (aQName.equals("colour")) {
			this.show.colour = this.text.toString();
		} else if (aQName.equals("aspect")) {
			this.show.aspect = this.text.toString();
		} else if (aQName.equals("quality")) {
			this.show.quality = this.text.toString();
		} else if (aQName.equals("stereo")) {
			this.show.audio = this.text.toString();
		} else if (aQName.equals("premiere")) {
			this.show.premiere = this.text.toString();
		} else  if(aQName.equals("icon")){
			// noop	
		} else if (aQName.equals("new")) {
			// noop
		} else if (aQName.equals("live")) {
			// noop
		} else if (aQName.equals("subtitles")) {
			// noop
		} else if (aQName.equals("value")) {
			this.value = this.text.toString();
		} else if (aQName.equals("rating")) {
			if (this.value != null && this.value.trim().length() > 0) addRatingToShow();
			this.ratingSystem = null;
			this.value = null;
		} else if (aQName.equals("star-rating")) {
			addStarRatingToShow();
			this.value = null;
		} else if (this.init.ChannelNumberTag!=null && aQName.equals(this.init.ChannelNumberTag) && XMLTV_Section.equals("channel")) {
			// Custom XMLTV tag to get channel number
			if (this.channel != null)this.channel.xmltvNumberTag=this.text.toString();
		} else if (!ignoreQName(aQName)) {
			String logText="</" + aQName + "> not being parsed";
			logDebug(logText);
			log(logText);

		}
		this.text = null;
		this.elementLanguage = null;
		
		} catch (Error error) {
			throw error;
		} catch (Exception t) {
			log("aQName_Start: " + aQName_Start  + " aQName " + aQName);
			log(t);
			recordImportFailure("XML element " + aQName, t);
			throw new SAXException("Unable to process XMLTV element " + aQName, t);
		}
	
    }

    /**
     * Adds the star rating to the show.
     */
    private final void addStarRatingToShow() {
        String[] strings = this.value.split(" */ *");
        if (strings.length > 1) {
            this.show.stars = (int) Math.round(Double.parseDouble(strings[0])
                    * this.init.maxStars
                    / Double.parseDouble(strings[1]));
        }
    }

    /**
     * Adds the rating data to the show.
     */
    private final void addRatingToShow() {
        String property = getProperty(this.configuration, "rating."
                + this.ratingSystem
                + "."
                + this.value);
        if (property == null) 
            property = getProperty(this.configuration, "rating.*." + this.value);
		//Replace TV with TV- and try again 
		if (property == null) 
            property = getProperty(this.configuration, "rating.*." + this.value.replaceAll("TV", "TV-"));	
        if (property == null) {
			String logText="Missing configuration: rating."
                    + this.ratingSystem
                    + "."
                    + this.value
                    + "=";
            logDebug(logText);
			log(logText);
			
        } else {
            String[] ratings = getStrings(property);
            if (ratings.length > 0) {
                if (ratings[0].length() > 0) {
                    if (this.show.rating == null
                            || RATINGS.indexOf(this.show.rating) > RATINGS
                                    .indexOf(ratings[0])) {
                        // A rating can only be overridden by a more restrictive 
                        // rating.
                        this.show.rating = ratings[0];
                    }
					if (ratings[0].startsWith("TV")) {
						this.show.parentalRating = ratings[0];
					}
                }
                for (int i = 1; i < ratings.length; ++i) {
                    if (ADVISORY_CONTENT_STRINGS.contains(ratings[i])) {
                        this.show.expandedRatings.add(ratings[i]);
                    }
                }
            }
        }
    }

    /**
     * Sets the XMLTV numbering system number on the show.
     * 
     * @param aEpisodeNumber the XMLTV numbering system number.
     */
    private final void parseEpisodeNumber(String aEpisodeNumber) {
        if ("xmltv_ns".equals(this.system)) {
            Matcher matcher = RE_EPISODE_NUMBER_PATTERN.matcher(aEpisodeNumber);
            if (matcher.find()) {
                this.show.season = toInt(matcher.group(1), -1) + 1; // 0-based
                this.show.seasons = toInt(matcher.group(2), 0);
                this.show.episode = toInt(matcher.group(3), -1) + 1; // 0-based
                this.show.episodes = toInt(matcher.group(4), 0);
                this.show.part = toInt(matcher.group(5), -1) + 1; // 0-based
                this.show.parts = toInt(matcher.group(6), 0);
                if (this.show.season > 0 || this.show.episode > 0) {
                    // Create a free-form version of the episode number.
                    StringBuffer sb = new StringBuffer();
                    if (this.show.season > 0) {
                        sb.append("S" + this.show.season);
                    }
                    if (this.show.episode > 0) {
                        sb.append("E" + this.show.episode);
                    }
                    this.show.freeFormEpisodeNumber = sb.toString();
                }
            } else {
                this.show.freeFormEpisodeNumber = aEpisodeNumber;
            }
			
        } else if 	( 	("xmltv:".equals(this.system)) || 
						("placeholder".equals(this.system))
					){
					// This is for XMLTV from channelDVR
					// <episode-num system="xmltv:">pluto/601311f067e9ba001a83e307/2007-05-22T00:00:00.000Z</episode-num>
					// <episode-num system="xmltv:">pluto/612d1d802274a5001329ff23/S2E1</episode-num>
					// <episode-num system="xmltv:">pluto/612d1d802274a5001329ff23/S2E3</episode-num>
					// <episode-num system="placeholder">placeholder/1094/1668488400</episode-num>
					
					String[] arrOfStr=aEpisodeNumber.split("/");
					if(arrOfStr.length>=3)
					{
						if("pluto".equals(arrOfStr[0]))
						{
							//Find date code which is not needed
							if(arrOfStr[2].indexOf("Z")>=0)
								this.show.showId = arrOfStr[1];
							else
								this.show.showId = arrOfStr[1] + "." + arrOfStr[2].replaceAll("00\\-\\+0000", "");
						}
						
						if("placeholder".equals(arrOfStr[0]))
						{
							this.show.showId = arrOfStr[1] + "." + arrOfStr[2];
						}
					}				
        } else if ("dd_progid".equals(this.system)) {
            this.show.showId = aEpisodeNumber.replaceFirst("\\.", ""); 
		} else if ("series".equals(this.system)) {
			// Some zap2xml feeds provide the series, season and episode as
			// separate episode-num elements. A series ID is not a show ID.
			this.show.seriesId = aEpisodeNumber;
		} else if ("season".equals(this.system)) {
			this.show.season = toInt(aEpisodeNumber, -1);
		} else if ("episode".equals(this.system)) {
			this.show.episode = toInt(aEpisodeNumber, -1);
		//tms and pluto here from previous versions.  Other custom can be set in .properties	
		} else if 	(	("tms".equals(this.system)) || 
						("plutod".equals(this.system))||
						(this.init.ProgrammeEpisodeNumSystemShowID_Value!=null &&
						 this.init.ProgrammeEpisodeNumSystemShowID_Value.equals(this.system))
					)
					{this.show.showId = aEpisodeNumber;
		} else if (("common".equals(this.system) || "onscreen".equals(this.system))) {
            this.show.freeFormEpisodeNumber = aEpisodeNumber;
		}else if ("original-air-date".equals(this.system)) {
            //NOP
        } else {
			//default case when system=unknown
			String logText="<programme><episode-num system=" + this.system + " /> /> xmltv.programme.episode-num.system.showID_Value if its the showID";
            logDebug(logText);
			log(logText);
        }
    }

    /**
     * Parses a string for an int.
     * <br>If the string is a <code>null</code> the default value will be used.
     * 
     * @param aString the string containing the int.
     * @param aDefault the default value.
     * @return the resulting int.
     */
    private static final int toInt(String aString, int aDefault) {
        if (aString == null) {
            return aDefault;
        }
        try {
            return Integer.parseInt(aString.trim());
        } catch (NumberFormatException e) {
            return aDefault;
        }
    }

	private boolean shouldUseLanguage(String existingLanguage, String incomingLanguage) {
		if (this.init.PreferredLanguage.length() == 0) return true;
		if (isPreferredLanguage(existingLanguage)) return false;
		return existingLanguage == null || isPreferredLanguage(incomingLanguage);
	}

	private boolean isPreferredLanguage(String language) {
		if (this.init.PreferredLanguage.length() == 0 || language == null) return false;
		String preferred = this.init.PreferredLanguage.replace('_', '-');
		String candidate = language.trim().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
		return candidate.equals(preferred) || candidate.startsWith(preferred + "-");
	}

    /**
     * Adds a person to the show.
     * 
     * @param aName the name of the person.
     * @param aRole the role.
     */
    private final void addPersonToShow(String aName, byte aRole, String aCharacter) {
        if (aRole > 0 && aName.length() > 0) {
            String[] names = aName
                    .split(" *(,|;| and | en | e(\\.a)??\\.??( |$)| \\([^\\)]*\\)) *");
            for (int i = 0; i < names.length; ++i) {
                if (names[i].length() > 0) {
                    this.show.people.add(names[i]);
                    this.show.roles.write(aRole);
					this.show.characters.add(aCharacter);
                }
            }
        }
    }

    /**
     * Adds the current channel.
     */
    private final void addChannelToGuide() {
	try {
		//RegEx to find Channel Number(No Text Just Number with/without decimal)
		String RegExChNum="^[\\+\\-]?\\d*[\\.\\-]?\\d+(?:[Ee][\\+\\-]?\\d+)?$";
		
		
		String ShortName=null;
		String LongName=null;
		String Network=null;
		String LongNumberName=null;
		String ShortNumberName=null;
		boolean InitNumberFound=false;
		int StationID;
		
		//User option to use NumberTag from .properties file for ChannelNumber
		if (this.init.ChannelNumberTag!=null)
		{
			String xmltvNumberTag=null;
			if(this.init.ChannelNumberTag.equals("channel")){
				//.properties xmltv.channel.NumberTagIndex=channel
				xmltvNumberTag=this.channel.xmltvId;
			} else if (this.init.ChannelNumberTag.equals("display-name")){
				//.properties xmltv.channel.NumberTagIndex=display-name
				//.properties xmltv.channel.NumberTagIndex=1
				if (this.init.ChannelNumberTagIndex>0 && this.channel.xmltvDisplayNames.size()>=this.init.ChannelNumberTagIndex)
				xmltvNumberTag=this.channel.xmltvDisplayNames.get(this.init.ChannelNumberTagIndex-1).toString();
			} else{
				//.properties xmltv.channel.NumberTagIndex=any_custom_tag
				xmltvNumberTag=this.channel.xmltvNumberTag;
			}	
			
			if (xmltvNumberTag != null && this.init.ChannelNumberTagPattern != null)
			{
				Matcher matcher = this.init.ChannelNumberTagPattern.matcher(xmltvNumberTag);
				if (matcher.find())
				{
					//Found RegEx Group 0
					xmltvNumberTag=matcher.group(0);	
				}					
			}
			if(xmltvNumberTag!=null)
			{
				//User option NumberTag found a Channel number
				this.channel.numbers.clear();
				this.channel.numbers.add(xmltvNumberTag);
			}
		}
		else //Find ChannelNumber(s) from xmltvDisplayNames
		{
			//Loop  all Display names to find channel numbers
			for (int i = 0; i < this.channel.xmltvDisplayNames.size(); i++) 
			{
				String xmltvDisplayName=this.channel.xmltvDisplayNames.get(i).toString();
					
				//<display-name> with Number(No Text Just Number with/without decimal)							
				if( xmltvDisplayName.matches(RegExChNum))
				{					
					this.channel.numbers.add(xmltvDisplayName);
				}
				else //<display-name>with Number and call name (EXP: 2.2 WBBMDT2
				{	
					String[] arrOfStr = xmltvDisplayName.split(" ");
					if(arrOfStr.length>=2 && arrOfStr[0].matches(RegExChNum))
					{
						//Number(No Text Just Number with/without decimal)		
						this.channel.numbers.add(arrOfStr[0]);
					}
				}
			}
		}
		

		
		// Override the values from .propties file if exist for names, network, numbers
		//channel.<key>.names=JZNET, Joels network testing
		//channel.<key>.network=ABC
		//channel.<key>.numbers=2.1
		//
		//<key> channel number or channel.xmltvId
		Channel InitChannel;
		//Check if xmltvId is the Channel Key for the channel Map
		InitChannel = this.init.channels.get(this.channel.xmltvId);
		
		//Check if the any of the numbers is Channel Key for the channel Map 
		Iterator<String> itrNumbers = this.channel.numbers.iterator();
		while(InitChannel==null && itrNumbers.hasNext())
		{
			InitChannel=this.init.channels.get(itrNumbers.next());
		}

		// Override the (names, network, numbers) from .propties file if InitChannel was found
		if (InitChannel != null)
		{
			// Override the names with 'channel.[id].xmltvDisplayNames'
			if (InitChannel.xmltvDisplayNames.size() > 0)
			{	
				this.channel.xmltvDisplayNames=InitChannel.xmltvDisplayNames;
			}
			// Add the 'channel.[id].numbers' from .properties
			if (InitChannel.numbers.size() > 0)
			{
				this.channel.numbers=InitChannel.numbers;
				InitNumberFound=true;
			}
			// Set the network to 'channel.[id].network'
			if (InitChannel.network!=null)this.channel.network=InitChannel.network;
		}
		
		//RegEx to find Short Name (All Caps numbers at end)
		for (int i = 0; i < this.channel.xmltvDisplayNames.size(); i++) 
		{
			String xmltvDisplayName=this.channel.xmltvDisplayNames.get(i).toString();
			String[] arrOfStr = xmltvDisplayName.split(" ");
			//<display-name> with Number(No Text Just Number with/without decimal)							
			if( xmltvDisplayName.matches(RegExChNum) && !InitNumberFound) 
				this.channel.numbers.add(xmltvDisplayName);
			//<display-name>with Number and call name (EXP: 2.2 WBBMDT2)
			else if(arrOfStr.length>=2 && arrOfStr[0].matches(RegExChNum))
			{	
				//Number(No Text Just Number with/without decimal)		
				if(!InitNumberFound)this.channel.numbers.add(arrOfStr[0]);
				//Short name
				ShortNumberName=arrOfStr[1];
				//Long Name with Number and call name (EXP: 2.2 WBBMDT2)
				LongNumberName=xmltvDisplayName;
			}
			//Short name based on RegEx
			else if( xmltvDisplayName.matches("[A-Z0-9-]+"))
			{
				ShortName=xmltvDisplayName;
			}
			//Long Name
			else LongName=xmltvDisplayName;
		}
		if(LongName==null)LongName=LongNumberName;
		if(ShortName==null)ShortName=ShortNumberName;		
		
		
		//User option to use ChannelShortNameIndex from .properties file
		if (this.init.ChannelShortNameIndex>0 && this.channel.xmltvDisplayNames.size()>=this.init.ChannelShortNameIndex)
			ShortName=this.channel.xmltvDisplayNames.get(this.init.ChannelShortNameIndex-1).toString();
		//User option to use fiter ShortNameRegex from .properties file
		if (ShortName != null && this.init.ChannelShortNamePattern != null)
		{
			Matcher matcher = this.init.ChannelShortNamePattern.matcher(ShortName);
			if (matcher.find())
			{
				//Found RegEx Group 0
				ShortName=matcher.group(0);	
			}
		}
		
		//User option to use ChannelLongNameIndex from .properties file
		if (this.init.ChannelLongNameIndex>0 && this.channel.xmltvDisplayNames.size()>=this.init.ChannelLongNameIndex)
			LongName=this.channel.xmltvDisplayNames.get(this.init.ChannelLongNameIndex-1).toString();
			
		//Set ShortName first number as no Short name was found
		if(ShortName==null && !this.channel.numbers.isEmpty())
			ShortName=this.channel.numbers.stream().findFirst().get().toString();	
	
		//Set Network to short name as should be the network all sign
		if(Network==null)	
		{
			Network=ShortName;
			this.channel.network=ShortName;
		}
		
		if(ShortName==null)ShortName="";
		if(LongName==null)LongName=ShortName;
		//Replace and not file chars for channel ICON
		String RegExInvalidChars="[^a-zA-Z0-9\\._\\s]+";
		LongName=LongName.replaceAll(RegExInvalidChars, "-");
        ShortName=ShortName.replaceAll(RegExInvalidChars, "-");			
				
		if (ChannelMapper.providerScope(this.initProviderId) == 999
				&& this.initProviderId != null && !"999".equals(this.initProviderId)) {
			log("Provider ID is outside the station-ID scope 1-999; scope 999 will be used.");
		}
		if (this.channel.numbers.isEmpty()) {
			log("XMLTV_ID: " + this.channel.xmltvId
					+ " has no channel number; using CRC16 for its station ID.");
		}
		StationID = ChannelMapper.stationId(this.initProviderId,
				this.channel.xmltvId, this.channel.numbers);
		String stationOwner = this.stationIdOwners.get(Integer.valueOf(StationID));
		if (stationOwner != null && !stationOwner.equals(this.channel.xmltvId)) {
			int collidedStationId = StationID;
			StationID = ChannelMapper.collisionStationId(this.initProviderId,
					this.channel.xmltvId, this.stationIdOwners.keySet());
			log("Station ID collision " + collidedStationId + " between " + stationOwner
					+ " and " + this.channel.xmltvId + "; assigned deterministic fallback "
					+ StationID + " to " + this.channel.xmltvId);
		}
		this.stationIdOwners.put(Integer.valueOf(StationID), this.channel.xmltvId);
		this.channel.STVstationID=StationID;
				
		this.channel.numbers = ChannelMapper.normalizeNumbers(this.channel.numbers,
				this.init.SagetvChannelNumberOffset, this.init.SagetvChannelNumberSeparator);
		
		if(xLogChannel)
			log(
				String.format( "ID: %1$10s " , StationID) + 
				String.format( "Numbers: %1$-28s " , this.channel.numbers) +
				String.format( "Short: %1$-8s " , ShortName) +
				String.format( "Long: %1$-42s " , LongName) +
				String.format( "Names: %1$-15s " , this.channel.xmltvDisplayNames) +
				""
				);

		/*	
		   * Call this to add a Channel to the database. This will update if the stationID is already
		   * used.  name should be the call sign, like KABC. longName can be a full descriptive name like
		   * "Channel 4 Los Angeles NBC". network represents the parent network, i.e. ABC, HBO, MTV and is
		   * optional. stationID is the GUID referring to this Channel.
		   *
		   * Returns true if the Channel was successfully updated/added to the database.

		  public boolean addChannelPublic(String name, String longName, String network, int stationID);
		 */
		if (!this.guideWriter.addChannel(ShortName, LongName, Network, StationID)) {
			throw new IllegalStateException("SageTV rejected channel " + this.channel.xmltvId);
		}
		
		
		if (this.channel.xmltvIcon != null && this.init.SagetvChannelIconDownload
				&& ShortName.length() > 0) {
			File imageFile = new File(new File("ChannelLogos"), ShortName + ".png");
			downloadAndNormalizeChannelIcon(this.channel.xmltvIcon, imageFile,
					this.init.SagetvChannelIconMaxWidth,
					this.init.SagetvChannelIconMaxHeight);
		}

		//Put the channel in XMLTV Map with the key being xmltvId String
		//This is used later for SageTV fuction aGuide.setLineup located in updateGuide function
		this.channels.put(this.channel.xmltvId, this.channel);	

			
        } catch (Error error) {
			throw error;
		} catch (Exception exception) {
			recordImportFailure("channel " + (this.channel == null ? "unknown" : this.channel.xmltvId), exception);
            log(exception);
        }
    }

    // TODO check to put the right title to SageTV correctly handling the rerun shows
    /**
     * Adds the current show.
     */
    private final void addShowToGuide() {
        if (this.channel != null && this.show != null && this.show.start != null
                && this.show.start.getTime() > (currentTimeMillis()-(8*3600000))
                && this.show.end != null
                && this.show.start.before(this.show.end)) {//Add show if it is upto 8 hours previous
            
            try {
                LinkedList<String> categories = translateCategories(this.show.categories);
                if (!this.init.categoriesForStarRating.isEmpty()
                        && this.show.stars >= 0) {
                    // Prevent duplication of categories.
                    categories.removeAll(this.init.categoriesForStarRating);
                    // Prepend all the categories that should be added for a 
                    // star-rating element. 
                    categories.addAll(0, this.init.categoriesForStarRating);
                }
                if (makeFirst(categories, "Movie")
                        || makeFirst(categories, "Series")
                        || makeFirst(categories, "News")
                        || makeFirst(categories, "Sports event")
                        || makeFirst(categories, "Sports non-event")
                        || makeFirst(categories, "Sports talk")) {
                    // The show is now colored.
                }

				// Resolve the existing identity before optional enrichment. This is
				// the invariant that keeps Show IDs, favourites, and watched history
				// identical when TMDB is toggled or temporarily unavailable.
				String identityCategory = categories.size() > 0 ? categories.get(0) : null;
				ShowIdGenerator.Result showIdentity = this.showIdGenerator.generate(
						this.show, identityCategory, this.channel);
				String showId = showIdentity.getExternalId();
				if (this.tmdbEnricher != null) {
					this.tmdbEnricher.enrichMissing(this.show, categories, identityCategory);
				}

                // Check for rerun
                if (!this.show.is_rerun) {
                    if (this.init.rerunAfterDate >= 0
                            && this.show.date != null
                            && isActivated(categories,
                                    this.init.rerunAfterDateCategories)) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(this.show.date);
                        calendar.add(Calendar.DATE, this.init.rerunAfterDate);
                        if (this.show.start.after(calendar.getTime())) {
                            this.show.is_rerun = true;
                        }
                    } else if (!this.show.is_rerun) {
                        if (this.show.episodeName == null
                                && this.show.episode <= 0
                                && this.show.freeFormEpisodeNumber == null
                                && isActivated(categories,
                                        this.init.rerunNoEpisodeCategories)) {
                            this.show.is_rerun = true;
                        } else if (this.show.date == null
                                && isActivated(categories,
                                        this.init.rerunNoDateCategories)) {
                            this.show.is_rerun = true;
                        }
                    }
                }

                String title = this.show.title;
                // FIXME handling rerun shows correctly with SageTV 9
                // String rerunTitle = null;
                String rerunTitle = this.show.title;
                String episodeName = this.show.episodeName;
				

                
				if (this.init.capAllChannelIds
                        || this.init.capChannelIds
                                .contains(this.channel.xmltvId)) {
                    if (this.init.capTitle) {
                        title = initcap(title);
                    }
                    if (this.init.capEpisodeName) {
                        episodeName = initcap(episodeName);
                    }
                }
                if (title != null) { 
					//Remove previos year if exist so their are no multiple shown
					if(this.show.year != null)title=title.replace(" (" + this.show.year + ")", ""); 
					
                    if (this.show.date != null && isActivated(categories,this.init.dateTitleDecorationCategories)) {
                        title = this.init.dateTitleDecoration.format(new Object[] {title, this.show.date},new StringBuffer(), null).toString();
                    } else if (this.init.titleAddYear && this.show.year != null && isActivated(categories, this.init.titleAddYearCategories) && !title.contains("(" + this.show.year + ")")) {
                        	
						title += " (" + this.show.year + ")";
                    }
					
                    if (this.show.quality != null && this.show.quality.startsWith("HD") && isActivated(this.channel.STVstationID,this.init.hdTitleDecorationChannels)) {
                        title = this.init.hdTitleDecoration.format(new Object[] {title}, new StringBuffer(), null).toString();
                    }
					
                }
                if (this.show.is_rerun) {
                    rerunTitle = title;
                    // FIXME handling rerun shows correctly with SageTV 9
                    //title = null;
                }
                if (this.init.episodeNameAddEpisodeNumber && this.show.freeFormEpisodeNumber != null) {
                    if (episodeName == null) {
                        episodeName = this.show.freeFormEpisodeNumber + " ";
                    } else {
                        episodeName = this.show.freeFormEpisodeNumber
                                + " "
                                + episodeName;
                    }
                }
				
                if (this.init.episodeNameAddPartNumber && this.show.part > 0) {
                    StringBuffer sb = new StringBuffer();
                    if (episodeName == null) {
                        sb.append("(" + this.show.part);
                    } else {
                        sb.append(episodeName + " (" + this.show.part);
                    }
                    if (this.show.parts > 0) {
                        sb.append("/" + this.show.parts);
                    }
                    sb.append(")");
                    episodeName = sb.toString();
                }

                String desc = this.show.descriptions.size() > 0 ? this.show.descriptions.get(0) : null;
				
				String category = categories.size() > 0 ? (String) categories.get(0) : null;
                String subCategory = null;
                if (categories.size() > 1) {
                    StringBuffer sb = new StringBuffer((String) categories
                            .get(1));
                    for (int i = 2; i < categories.size()
                            && i <= this.init.maxSubcategories; ++i) {
                        sb.append(" / " + categories.get(i));
                    }
                    subCategory = sb.toString();
                }

                List<String> bonus = new LinkedList<String>();
                if (this.show.descriptions.size() > 1) {
                    bonus.addAll(this.show.descriptions.subList(1, this.show.descriptions.size()));
					}
                if (this.show.stars > 0) {
                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < this.show.stars; ++i) {
                        sb.append('*');
                    }
                    bonus.add(sb.toString());
                }
                if (this.show.aspect != null) {
                    bonus.add(this.show.aspect);
                }
                if ("no".equals(this.show.colour)) {
                    bonus.add("B&W");
                }
                if (this.show.quality != null) {
                    bonus.add(this.show.quality);
                }
                if (this.show.audio != null) {
                    bonus.add(initcap(this.show.audio));
                }
                if (!this.show.countries.isEmpty()) {
                    StringBuffer sb = new StringBuffer();
                    Iterator<String> it = this.show.countries.iterator();
                    if (it.hasNext()) {
                        sb.append(it.next());
                        while (it.hasNext()) {
                            sb.append("/" + it.next());
                        }
                    }
                    bonus.add(sb.toString());
                }
                if (this.show.originalLanguage != null && !this.show.originalLanguage.equals(this.show.language)) {
                    bonus.add("Original language: " + this.show.originalLanguage);
                }
                if (this.show.season > 0) {
                    bonus.add("Season " + this.show.season + (this.show.seasons > 0 ? "/" + this.show.seasons : ""));
                } else if (this.show.seasons > 0) {
                    bonus.add(this.show.seasons + " seasons");
                }
                if (this.show.episode > 0) {
                    bonus.add("Episode "  + this.show.episode + (this.show.episodes > 0 ? "/" + this.show.episodes : ""));
                } else if (this.show.episodes > 0) {
                    bonus.add(this.show.episodes + " episodes");
                }
                if (this.show.part > 0) {
                    bonus.add("Part "+ this.show.part + (this.show.parts > 0 ? "/" + this.show.parts : ""));
                } else if (this.show.parts > 0) {
                    bonus.add(this.show.parts + " parts");
                }

				String showIdText = "Show ID: " + showId;
				if (this.init.ShowIdDisplay.equals("description")) {
					desc = desc == null || desc.length() == 0
							? showIdText : desc + NEWLINE + NEWLINE + showIdText;
				} else if (this.init.ShowIdDisplay.equals("bonus")) {
					bonus.add(showIdText);
				}
				
				int stationId = this.channel.STVstationID.intValue();
				long start = this.show.start.getTime();
				long duration = this.show.end.getTime() - start;
				
				long originalAirDate=0;
				if(this.show.date != null)originalAirDate=this.show.date.getTime();	
				ProgrammeMapper.AiringFields airingFields = ProgrammeMapper.map(this.show);
			
				String parentalRating=this.show.parentalRating;
				/*  
				   * Call this to add a Show to the database. If a show with this extID is already present, it will be updated
				   * to this information. You can use null or String[0] for any fields you don't want to specify.
				   * title - the title of the show
				   * episodeName - the name of the episode
				   * desc - a description of this show
				   * duration - not used, set to 0
				   * categories - categories for this show
				   * people - names of people/actors in this show
				   * roles - must be same length as people array, uses the X_ROLE constants in this file to specify what each is
				   * rated - rating of a show, i.e. PG, G, R, etc.
				   * expandedRatings - additional rating information, i.e. Violence, Nudity, Adult Content
				   * year - the year it was produced, for movies
				   * parentalRating - not used, set to null
				   * bonus - additional information about the show
				   * extID - GUID representing this show
				   * language - the language the show is in
				   * originalAirDate - the original airing date of this show, it's a long value from java.util.Date
				   * seasonNum - the season number, 0 if this is undefined
				   * episodeNum - the season number, 0 if this is undefined
				   * forcedUnique - true if it's known that this Show represents 'unique' content (i.e. all Airings with a Show of this ExternalID will be the EXACT same content)
				   *
				   * Returns true if the Show was successfully updated/added to the database.
				   
				  public boolean addShowPublic2(String title, String episodeName, String desc, long duration, String[] categories,
					  String[] people, byte[] roles, String rated, String[] expandedRatings,
					  String year, String parentalRating, String[] bonus, String extID, String language, long originalAirDate,
					  short seasonNum, short episodeNum, boolean forcedUnique);  
				*/
				if(!this.guideWriter.addShow(title, episodeName, desc, duration, toStringArray(categories),
				  toStringArray(this.show.people), this.show.roles.toByteArray(), this.show.rating, toStringArray(this.show.expandedRatings),
				  this.show.year, parentalRating, toStringArray(bonus), showId, this.show.language, originalAirDate,
				  airingFields.season, airingFields.episode))
				  {
						throw new RuntimeException("Add show failed.");
				  }
				  else
				  {
					/*
					* Call this to add a SeriesInfo object to the database. If a SeriesInfo with this seriesID is already present, it will be updated
					* to this information. You can use null or String[0] for any fields you don't want to specify.
					* seriesID - the ID of the series, this should match the prefix of corresponding ShowIDs w/out the last 4 digits for proper linkage (i.e. the SeriesID for EP1234567890 would be 123456)
					* title - the title of the series
					* network - the network that airs the series
					* description - a description of this series
					* history - a historical description of the series
					* premiereDate - a String representation of the date the series premiered
					* finaleDate - a String representation of the date the series ended
					* airDOW - a String representation of the day of the week the series airs
					* airHrMin - a String representation of the time the series airs
					* imageURL - a URL that links to an image for this series
					* people - names of people/actors in this show
					* characters - must be same length as people array, should give the character names the corresponding people have in the series
					*
					* Returns true if the SeriesInfo was successfully updated/added to the database.
					
					public boolean addSeriesInfoPublic(int seriesID, String title, String network, String description, String history, String premiereDate,
					String finaleDate, String airDOW, String airHrMin, String imageURL, String[] people, String[] characters);  
					*/
					
					
					Integer seriesID = showIdentity.getSeriesId();
					if (seriesID != null) {
					try {
						//`.properties` option 'sagetv.show.Icon'	
						String XMLTVicon="";
						if(this.init.SagetvShowIcon)XMLTVicon=this.show.xmltvIcon;
						
						if (!this.guideWriter.addSeries(seriesID.intValue(), title, XMLTVicon,
								toStringArray(this.show.people), toStringArray(this.show.characters))) {
							throw new IllegalStateException("SageTV rejected SeriesInfo " + seriesID);
						}
						
					} catch (Exception exception) {
						recordImportFailure("SeriesInfo " + seriesID, exception);
						log(exception);
					}
					}
					

					  
				  }
				
				/* 	
				public static final int CC_MASK = 0x01;
				public static final int STEREO_MASK = 0x02;
				public static final int HDTV_MASK = 0x04;
				public static final int SUBTITLE_MASK = 0x08;

				public static final int PREMIERES_BITMASK = 0x70;
				public static final int PREMIERE_MASK = 0x10;
				public static final int SEASON_PREMIERE_MASK = 0x20;
				public static final int SERIES_PREMIERE_MASK = 0x30;
				public static final int CHANNEL_PREMIERE_MASK = 0x40;
				public static final int SEASON_FINALE_MASK = 0x50;
				public static final int SERIES_FINALE_MASK = 0x60;

				public static final int SAP_MASK = 0x80;
				public static final int THREED_MASK = 0x100;
				public static final int DD51_MASK = 0x200;
				public static final int DOLBY_MASK = 0x400;
				public static final int LETTERBOX_MASK = 0x800;
				public static final int LIVE_MASK = 0x1000;
				public static final int NEW_MASK = 0x2000;
				public static final int WIDESCREEN_MASK = 0x4000;
				public static final int SURROUND_MASK = 0x8000;
				public static final int DUBBED_MASK = 0x10000;
				public static final int TAPE_MASK = 0x20000;

				* Call this to add an Airing to the database. An Airing is time-channel-show correlation.
				* extID - refers to the GUID of a Show previously added with addShowPublic
				* stationID - referes to the stationID GUID of a Channel previously added with addChannelPublic
				* startTime - the time this airing starts, a long from java.util.Date
				* duration - the length of this airing in milliseconds
				* partsByte - the highest 4 bits should be the part number of this airing, and the lowest four bits should be the total parts; set this to zero if it's not a multipart Airing
				* misc - integer bitmask of other misc. properties; see above for the constants used here, for 'premiere/finale' info it uses 3 bits for that one value
				* parentalRating - the parental rating for the show, should be a localized value from "TVY", "TVY7", "TVG", "TVPG", "TV14", "TVM" or the empty string
				*
				* Returns true if this Airing was successfully updated/added to the database. The database will
				* automatically ensure that there are no inconsistencies in the Airings, if you add one that
				* overlaps with the station-time-duration of another. The one(s) that were in the database before
				* the call will be removed and the new one added. It will also fill in any gaps with "No Data" sections
				* for you automatically.

				public boolean addAiringPublic2(String extID, int stationID, long startTime, long duration, byte partsByte, int misc, String parentalRating);
				}
												
				*/	
				if (this.guideWriter.addAiring(showId, stationId, start, duration,
						airingFields.multipart, airingFields.miscellaneous, parentalRating))
				{
					if(xLogShow)log(
						String.format( "SID: %1$12s " , showId) + 
						String.format( "CID: %1$10s " , stationId) + 
						String.format( "Start: %1$-15s " , new SimpleDateFormat("dd-MM-yyyy HH:mm").format(start)) +
						String.format( "Duration: %1$-8s " ,   duration) +
						"Title: " + this.show.title
					);
				}
				else
				{
					throw new RuntimeException("Add airing failed.");
				}	

            } catch (Error error) {
				throw error;
			} catch (Exception exception) {
				recordImportFailure("programme " + (this.show == null ? "unknown" : this.show.title), exception);
				log(exception);
            }
        }
		else
			if(xLogShow)log("Show Older than 8 hours: " + this.show.toString());
			
    }

	private void recordImportFailure(String phase, Throwable failure) {
		if (this.importResult != null) this.importResult.fail(phase, failure);
	}

    /**
     * Routine to examine if a configuration item is activated.
     * 
     * @param aList the list of properties that is checked.
     * @param aActivatedBy the list of properties that activates the configuration item.
     * @return <code>true</code> if the configuration item is activated.
     */
    static final boolean isActivated(List<?> aList, List<?> aActivatedBy) {
        if (aActivatedBy.contains("*")) {
            return true;
        }
        for (Iterator<?> it = aActivatedBy.iterator(); it.hasNext();) {
            if (aList.contains(it.next())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Routine to examine if a configuration item is activated.
     * 
     * @param aItem the property that is checked.
     * @param aActivatedBy the list of properties that activates the configuration item.
     * @return <code>true</code> if the configuration item is activated.
     */
    static final boolean isActivated(Object aItem, List<?> aActivatedBy) {
        return aActivatedBy.contains(aItem.toString())
                || aActivatedBy.contains("*");
    }

    /**
     * Creates a string array from a collection.
     * 
     * @param aCollection the collection that should be converted.
     * @return the string array.
     */
    private static final String[] toStringArray(Collection<String> aCollection) {
        return aCollection.toArray(DUMMY_STRING_ARRAY);
    }

    /**
     * Initcaps a string.
     * 
     * @param aString the string to process.
     * @return the processed string.
     */
    private final String initcap(String aString) {
        Matcher lowercaseMatcher = LOWERCASE_WORDS_PATTERN.matcher(aString);
        char[] chars = aString.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        int i;
        while (lowercaseMatcher.find()) {
            if (!this.init.capSkipWords.contains(lowercaseMatcher.group(1))) {
                i = lowercaseMatcher.start(1);
                chars[i] = Character.toUpperCase(chars[i]);
            }
        }
        return new String(chars);
    }

    public final void endPrefixMapping(String aPrefix) throws SAXException {
    // noop
    }

    public final void ignorableWhitespace(char[] aCh, int aStart, int aLength)
            throws SAXException {
    // noop
    }

    public final void setDocumentLocator(Locator aLocator) {
    // noop
    }

    public final void skippedEntity(String aName) throws SAXException {
    // noop
    }

    /**
     * Parse an XMLTV date.
     * 
     * @param aXmltvDate the XMLTV formatted date
     * @return the parsed date.
     */
    private static final Date parseXmltvDate(String aXmltvDate) {
		return XmltvDateParser.parseTimestamp(aXmltvDate);
    }

    public final void startPrefixMapping(String aPrefix, String aUri)
            throws SAXException {
    // noop
    }

    public final void processingInstruction(String aTarget, String aData)
            throws SAXException {
    // noop
    }

    public final void error(SAXParseException aException) throws SAXException {
        log(aException);
		throw aException;
    }

    public final void fatalError(SAXParseException aException)
            throws SAXException {
        log(aException);
		throw aException;
    }

    public final void warning(SAXParseException aException) throws SAXException {
        log(aException);
    }

    /**
     * Translate a set of categories.
     * 
     * @param aCategories the categories that should be translated.
     * @return the translated categories.
     */
    public final LinkedList<String> translateCategories(Set<String> aCategories) {
        LinkedList<String> translatedCategories = new LinkedList<String>();
        if (!aCategories.isEmpty()) {
            LinkedList<String> notYetTranslated = new LinkedList<String>();
            notYetTranslated.addAll(aCategories);

            // Process the largest number of categories first.
            translation: while (notYetTranslated.size() > 0) {
                for (int length = Math.min(this.init.maxCategoryTranslationLength,
                        notYetTranslated.size()); length > 0; --length) {
                    List<String> subList = notYetTranslated.subList(0, length);
                    List<String> translations = this.init.categoryTranslations
                            .get(subList);
                    if (translations != null) {
                        for (Iterator<String> it = translations.iterator(); it
                                .hasNext();) {
                            String category = it.next();
                            if (!translatedCategories.contains(category)) {
                                translatedCategories.add(category);
                            }
                        }
                        subList.clear();
                        continue translation;
                    }
                }
                String category = notYetTranslated.removeFirst();
                if (!translatedCategories.contains(category)) {
                    translatedCategories.add(category);
                }
                if (this.init.reportMissingTranslateCategory) {
					String logText="Missing configuration: translate.category."
                            + category.replaceAll(" ", "\\\\ ").replaceAll("'",
                                    "\\\\'")
                            + "=";
                    logDebug(logText);
					log(logText);
                    // Only report each category once.
                    List<String> list = new LinkedList<String>();
                    list.add(category);
                    this.init.categoryTranslations.put(list, list);
                }
            }
        }
        return translatedCategories;
    }

    /**
     * Make a category the first category in the list.
     * 
     * @param aList the list of categories.
     * @param aCategory the category.
     * @return true if the category is first in the list.
     */
    private static final boolean makeFirst(LinkedList<String> aList, String aCategory) {
        if (aList.contains(aCategory)) {
            if (aList.indexOf(aCategory) > 0) {
                aList.remove(aCategory);
                aList.addFirst(aCategory);
            }
            return true;
        }
        return false;
    }
	
public static int CRC16_CCITT(byte[] buffer)
{
	return ChannelMapper.crc16(buffer);
}

public static void copyFileContentAppend(File a, File b)
	throws Exception
{
	try (FileInputStream in = new FileInputStream(a);
		 FileOutputStream out = new FileOutputStream(b, true)) {
		int n;
		// read() function to read the
		// byte of data
		while ((n = in.read()) != -1) {
			// write() function to write
			// the byte of data
			out.write(n);
		}
	}
}
  
	
}

