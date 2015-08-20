#!/bin/sh

exec soffice --nologo --headless --nofirststartwizard --accept='socket,host=127.0.0.1,port=2220,tcpNoDelay=1;urp;StarOffice.Service'
