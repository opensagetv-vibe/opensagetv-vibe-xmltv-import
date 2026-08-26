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
##2.03  01/14/2022
1.  Added String xmltvIcon to store Channel ICON

 */
package xmltv;

import java.util.List;
import java.text.MessageFormat;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Init data from .propties file

 * 
 */
public final class Init {
	
	    /**
     * The channels and programmes that are added to the guide can be limited to 
     * specified channel id's (comma separated list).
     *
     * If this setting is empty all channels and programs will be added.
     * NOTE: filtered channels will still be parsed, they will merely not be 
     * added to the guide.
     *
     * The default is:
     *	 channel.ids=
     */
    List<String> channelIds;
	
	
	//All parsed channels. from .properties file
    HashMap<String, Channel> channels = new HashMap<String, Channel>();

     /**
     ChannelShortNameIndex
	 default=0 with no entry .properties file
	 init.ChannelShortNameIndex0:=shortest found without spaces
	 init.ChannelShortNameIndex>0: XMLTV file index found in order starting with 1 <display-name>CBS</display-name>
     */
    int ChannelShortNameIndex;

	/**
    ChannelNumberTagRegEx	
	 default=.* with no entry .properties file will find anything
     */
    String ChannelShortNameRegEx;
	Pattern ChannelShortNamePattern;
	
	/**
     ChannelLongNameIndex
	 default=0 with no entry .properties file
	 init.ChannelLongNameIndex0:=longest found with spaces
	 init.ChannelLongNameIndex>0: XMLTV file index found in order starting with 1 <display-name>CBS</display-name>
     */
    int ChannelLongNameIndex;
	
	/**
     ChannelNumberSeparator
	 Used for SageTv display number 5.1 or 5-2
     */
    String SagetvChannelNumberSeparator;
	
	
	/**
     ChannelNumberOffset
	 Channel offset for channel numbers
     */
    int SagetvChannelNumberOffset;


	/**
     initChannelNumberTag
	 default="" No entry .properties file.  
	 initChannelNumberTag="" None will try to detect channel number from <channel id=XXX> or <display-name>XXX<display-name>
	 initChannelNumberTag=channel then <channel id=XXX> 
	 initChannelNumberTag=TAG_NAME then <TAG_NAME>xxx<TAG_NAME>
     */
    String ChannelNumberTag;
	
	
	/**
     initChannelNumberTagIndex
	 default=0 with no entry .properties file
	 initChannelNumberTagIndex=0 None will try to detect channel number from <channel id=xxx> or <display-name>
	 initChannelNumberTagIndex>0 XMLTV file index found in order starting with 1
     */
    int ChannelNumberTagIndex;
	
	/**
     initChannelNumberTagRegEx	
	 default=.* with no entry .properties file will find anything
     */
    String ChannelNumberTagRegEx;
	Pattern ChannelNumberTagPattern;
	
	/**
     ProgrammeEpisodeNumSystemShowID_Value	
	 custom id for show ID
	  <episode-num system="custom_id">MV01884821.0000</episode-num>
     */
	String ProgrammeEpisodeNumSystemShowID_Value;

	/** Optional location where the generated Show ID is exposed in SageTV. */
	String ShowIdDisplay;

	/** Maximum accumulated text for one XML element. */
	int XmltvMaxElementChars;
	String PreferredLanguage;
	
	/**
     sagetv.ShowIcon
	 default=false with no entry .properties
	 false=Do not add show Icon to SageTV database
	 true=add show Icon to SageTV database
     */
    boolean SagetvShowIcon;
	
	/**
     sagetv.channel.IconDownload
	 default=false with no entry .properties
	 false=Do Note download image file
	 true=Download image file from channel <icon src= >
     */
	boolean SagetvChannelIconDownload;

	/** Maximum dimensions of a downloaded local channel logo. */
	int SagetvChannelIconMaxWidth;
	int SagetvChannelIconMaxHeight;

    /**
     * Titles on dutch channels are capitalized differently from titles on
     * english channels. This would be ok if sage's favorites weren't
     * case-sensitive. To solve this the parser can capitalize the initial
     * letter of each word in a title. Since in english not all words are
     * capitalized there is also a list of words that can be excluded from this.
     * This also helps in IMDB lookups. If initcap.channel.ids is empty it is
     * used for all channels.
     * 
     * The defaults for are: initcap.title=false initcap.episodename=false
     * initcap.channel.ids= initcap.skip.words=the, a, an, at, in, on, and, of,
     * from, to, is, with, en, de, der, het, op, voor
     */
    boolean capTitle;
	

    /**
     * @see #initcapTitle
     */
    boolean capEpisodeName;

    /**
     * @see #initcapTitle
     */
    List<String> capChannelIds;

    /**
     * Flag to indicate that all channel ids should be initcapped.
     */
    boolean capAllChannelIds;

    /**
     * @see #initcapTitle
     */
    List<String> capSkipWords;

    /**
     * The year of the show is added to the title between braces. This should
     * help in IMDB lookups, but it doesn't (it doesn't hurt tough). NOTE: This
     * does not affect the generated show-id. If title.add.year.categories is
     * empty it is used for all categories.
     * 
     * The defaults for are: title.add.year=true title.add.year.categories=Movie
     */
	 boolean titleAddYear;
	 
	 
	 /**
     * @see #titleAddYear
     */
    List<String> titleAddYearCategories;
	
	    /**
     * The HD title decoration.
     */
    MessageFormat hdTitleDecoration;

    /**
     * The channels for which the titles can be HD decorated.
     */
    List<String> hdTitleDecorationChannels;
	
	 /**
     * The sage role for a director.
     */
    byte directorRole;
	
	/**
     * The sage role for an actor.
     */
    byte actorRole;

	    /**
     * The sage role for a writer.
     */
    byte writerRole;

    /**
     * The sage role for an adapter.
     */
    byte adapterRole;

    /**
     * The sage role for a producer.
     */
    byte producerRole;

    /**
     * The sage role for a presenter.
     */
    byte presenterRole;

    /**
     * The sage role for a commentator.
     */
    byte commentatorRole;

    /**
     * The sage role for a guest.
     */
    byte guestRole;

    /**
     * The category translations.
     */
    Map<List<String>, List<String>> categoryTranslations;

    /**
     * The maximum number of category combinations that are present in the rules.
     */
    int maxCategoryTranslationLength;

    /**
     * The maximum number of subcategories passed to Sage.
     */
    int maxSubcategories;

    /**
     * <code>true</code> if missing translate.category entries are detected.
     */
    boolean reportMissingTranslateCategory;

    /**
     * The number of days that should have passed since the show.date to mark 
     * the airing as a rerun.
     */
    int rerunAfterDate;

    /**
     * The categories for which init.rerunAfterDate should be applied.
     */
    List<String> rerunAfterDateCategories;

    /**
     * The categories for which a rerun is assumed if there is no episode 
     * information.
     */
    List<String> rerunNoEpisodeCategories;

    /**
     * The categories for which a rerun is assumed if there is no date 
     * information.
     */
    List<String> rerunNoDateCategories;

    /**
     * The categories are not always enough to determine if something is a movie.
     * This option allows you to add a category (or categories) if a star-rating 
     * exists. 
     */
    List<String> categoriesForStarRating;

    /**
     * The date year decoration.
     */
    MessageFormat dateYearDecoration;

    ////////////////////////////////////////////////////////////////////////////

	    /**
     * The date title decoration.
     */
    MessageFormat dateTitleDecoration;

    /**
     * The categories for which the titles can be date decorated.
     */
    List<String> dateTitleDecorationCategories;
	
	    /**
     * The episode number is prepended to the episode name. The part number of
     * the episode is added to the episode name NOTE: This does not affect the
     * generated show-id.
     * 
     * The default is: episodename.add.episode.number=true
     * episodename.add.part.number=true
     */
    boolean episodeNameAddEpisodeNumber;

    /**
     * @see #init.episodeNameAddEpisodeNumber
     */
    boolean episodeNameAddPartNumber;
	
	    /**
     * The "split.movie.detect.time" setting controls the maximum time (in
     * milliseconds) that two parts can be apart for them to be considered split
     * movie parts. Otherwise they are considered reruns of the same show. The
     * default value is 4 hours (14400000 milliseconds).
     */
    long splitMovieDetectTime;
	
	 /**
     * The maximum number of stars.
     */
    int maxStars;

}
