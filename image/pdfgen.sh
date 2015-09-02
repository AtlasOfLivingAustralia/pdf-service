#!/bin/sh
# `/sbin/setuser memcache` runs the given command as the user `memcache`.
# If you omit that part, the command will be run as root.
exec /sbin/setuser pdfgen java -Djava.awt.headless=true -jar /opt/pdfgen/pdfgen.jar server /etc/pdfgen/config.yml #>> /var/log/pdfgen.log 2>&1