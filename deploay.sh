#!/bin/sh

set -eux

mvn clean package
sudo systemctl stop tomcat
sudo rm -rf /var/lib/tomcat/webapps/resultsview*
sudo cp target/resultsview.war /var/lib/tomcat/webapps/
sudo chown tomcat:tomcat /var/lib/tomcat/webapps/resultsview.war
sudo systemctl start tomcat
mvn clean
curl http://localhost:8080/resultsview/runs > /dev/null 2>&1
