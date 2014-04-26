package com.ardoq.util;

/**
 * Created by magnulf on 24.04.14.
 */
public class SimpleMarkdownUtil {
    public static String htmlToMarkdown(String description) {
        description = description.replaceAll("<h\\d>(.*?)</h\\d>","###$1\n");
        description = description.replaceAll("<b>(.*?)</b>","**$1**");
        description = description.replaceAll("<i>(.*?)</i>","*$1*");
        description = description.replaceAll("<pre>(.*?)</pre>","```$1```");
        description = description.replaceAll("<code>(.*?)</code>","```\n$1\n```");
        description = description.replaceAll("<p>(.*?)</p>","\n$1\n\n");
        description = description.replaceAll("<br.*?>","\n");
        description = description.replaceAll("</{0,1}\\w+?>","");
        return description;
    }
}
