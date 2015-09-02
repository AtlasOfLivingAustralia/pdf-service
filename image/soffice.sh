#!/bin/sh
exec /sbin/setuser soffice soffice --nologo --headless --nofirststartwizard --accept='socket,host=0.0.0.0,port=2220,tcpNoDelay=1;urp;StarOffice.Service'
