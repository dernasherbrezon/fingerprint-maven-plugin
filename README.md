maven-fingerprint-plugin
========================

Maven Plugin for Fingerprinting Web Resources

= About =
Fingerprint is a generation of md5 sum of the whole file. 
Fingerprinting used to improve web resource caching. For example: rename file from `file.css` to `<md5>file.css`, where `<md5>` is a md5 of the whole file.

This plugin filters out (recursivly) source directory, detects any resources using the patterns below and copy result (if needed) to target directory.

The following patterns are used to detect resources to fingerprint:
  * {{{ <link.*?href="(.*?)".*?> }}}
  * {{{ "([^\\s]*?\\.js)" }}}
  * {{{ <img.*?src="(.*?)".*?> }}}
  * {{{ url\("(.*?)"\) }}}

After fingerprinting it is safe to add max expires header. 

= Requirements = 

  * All resources should have absolute paths:
    * Valid: {{{ <img src="/img/test.png"> }}}. 
    * Invalid: {{{ <img src="test.png"> }}}
  * All resources should point to existing files without any pre-processing:
    * Valid: {{{ <img src="/img/test.png"> }}}. 
    * Invalid: {{{ <img src="<c:if test="${var}">/img/test.png</c:if>" }}}

= Configuration =
  # Add maven repo:
{{{
  <pluginRepositories>
		<pluginRepository>
                        <id>fprint-repo</id>
                        <url>https://raw.github.com/dernasherbrezon/maven-fingerprint-plugin/master/maven-fingerprint-plugin/mvn-repo</url>
		</pluginRepository>
	</pluginRepositories>
}}}
  # Configure plugin in pom.xml. Example configuration with comments:
{{{
				<configuration>
					<excludeResources>
						<excludeResource>://</excludeResource>
						<excludeResource>//</excludeResource>
					</excludeResources>
<!-- ${basedir}/src/main/webapp by default -->
					<sourceDirectory>${basedir}/target/webcombined</sourceDirectory>
<!-- ${project.build.directory}/fingered-web by default -->
					<outputDirectory>${basedir}/target/fingered</outputDirectory>
<!-- Remove unnecessary spaces between tags. Make single line page. Takes into consideration <pre> tags -->
					<trimTagExtensions>
						<trimTagExtension>html</trimTagExtension>
					</trimTagExtensions>
					<extensionsToFilter>
						<extensionToFilter>html</extensionToFilter>
						<extensionToFilter>jsp</extensionToFilter>
						<extensionToFilter>tag</extensionToFilter>
						<extensionToFilter>css</extensionToFilter>
						<extensionToFilter>js</extensionToFilter>
					</extensionsToFilter>
<!-- cdn host. Not required. For example using "//accountname.r.worldssl.net": /css/bootstrap.css -> //accountname.r.worldssl.net/css/<md5>bootstrap.css -->
					<cdn>${cdn}</cdn>
				</configuration>
}}}
  # Configure apache proxy or nginx to add max expires header. The following example configuration for nginx:
{{{
        location ~ ^/.+\.(ico|jpg|jpeg|gif|pdf|jar|png|js|css|txt|epf|ttf|svg|woff)$ {
            root         <your root content>;
            expires max;
            add_header Cache-Control public;
        }
}}}

