# WCAGgrader

Currently only works when executing from the root folder since access_lint is not linked dynamically.  
Right now, plugin does not work correctly since it can't find the JSONObject methods.  

    java.lang.NoSuchMethodError: org.json.JSONObject.keySet()Ljava/util/Set;
        at ee.ut.cs.Parser.accessLint(Parser.java:39)
        at org.apache.nutch.parse.htmlraw.HtmlRawParser.filter(HtmlRawParser.java:77)
        at org.apache.nutch.parse.HtmlParseFilters.filter(HtmlParseFilters.java:67)
        at org.apache.nutch.parse.html.HtmlParser.getParse(HtmlParser.java:197)
        at org.apache.nutch.parse.ParseUtil.parse(ParseUtil.java:82)
        at org.archive.nutchwax.Importer.output(Importer.java:440)
        at org.archive.nutchwax.Importer.importRecord(Importer.java:342)
        at org.archive.nutchwax.Importer.map(Importer.java:210)
        at org.archive.nutchwax.Importer.map(Importer.java:98)
        at org.apache.hadoop.mapred.MapRunner.run(MapRunner.java:50)
        at org.apache.hadoop.mapred.MapTask.run(MapTask.java:342)
        at org.apache.hadoop.mapred.LocalJobRunner$Job.run(LocalJobRunner.java:138)


