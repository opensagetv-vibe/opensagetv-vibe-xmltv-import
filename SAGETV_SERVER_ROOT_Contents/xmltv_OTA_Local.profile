# Example zap2xml/OTA channel layout. Copy and adjust for the provider feed.
include=common.properties
xmltv.channel.display-name.ShortNameIndex=1
xmltv.channel.display-name.ShortNameRegex=(?<=\\s).*
xmltv.channel.display-name.LongNameIndex=1
xmltv.channel.NumberTag=channel
xmltv.channel.NumberTagIndex=2
xmltv.channel.NumberTagRegEx=(?<=I)(.*)(?=\\.\\d+)
