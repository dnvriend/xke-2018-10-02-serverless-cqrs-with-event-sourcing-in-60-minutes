
lazy val `serverless-cqrs` = (project in file("."))
	//.disablePlugins(SAMPlugin, AwsPlugin)
	.aggregate(
        `cqrs`,
        `event-data-segment`,
	)

lazy val `cqrs` = project in file("cqrs")
    //.enablePlugins(SAMPlugin)
  
lazy val `event-data-segment` = project in file("event-data-segment")
    //.enablePlugins(SAMPlugin)

