FROM centos:7
RUN yum install -y java-1.7.0-openjdk
ADD "http://dev.redboxresearchdata.com.au/nexus/service/local/artifact/maven/redirect?r=snapshots&g=au.com.redboxresearchdata.redbox&a=redbox-api-base&v=LATEST&e=war" redbox-api-base.war
COPY . /mint-csv-uploader
CMD java -Denv=production -DredboxApiConfig=/mint-csv-uploader/config.groovy -jar redbox-api-base.war
