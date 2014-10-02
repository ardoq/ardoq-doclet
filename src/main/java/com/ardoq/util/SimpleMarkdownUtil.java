package com.ardoq.util;

/**
 Simple JavaDoc HTML to Markdown conversion....
 */
public class SimpleMarkdownUtil {
    public static String htmlToMarkdown(String description) {

      /*  description = description.replaceAll("\u003c", "<");
        description = description.replaceAll("\u003e", ">");*/
        description = description.replaceAll("<h\\d>(.*?)</h\\d>","###$1\n");
        description = description.replaceAll("<b>(.*?)</b>","**$1**");
        description = description.replaceAll("<i>(.*?)</i>","*$1*");
        description = description.replaceAll("<pre>(.*?)</pre>","```$1```");
        description = description.replaceAll("<code>(.*?)</code>","```\n$1\n```");
        description = description.replaceAll("<p>(.*?)</p>","\n$1\n\n");
        description = description.replaceAll("<br.*?>","\n");
        description = description.replaceAll("\\{@code(.*?)\\}","```$1```");
        description = description.replaceAll("</{0,1}\\w+?>","");
        description = description.replaceAll("<\\w+/{0,1}?>","");
        //description = description.replaceAll("\u003c/{0,1}\\w+?\u003e","");


        description = description.replaceAll("[<\u003c]","&lt;");
        description = description.replaceAll("[>\u003e]","&gt;");

        return description;
    }
}
