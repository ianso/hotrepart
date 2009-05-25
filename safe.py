#!/usr/bin/python
import uuid
import string
escapeToken = uuid.uuid4().hex
fIn = open("sql/items0.sql.unsafe")
fOut = open("sql/items0.safe.sql", "w")
try:
	for line in fIn:
		fOut.write(string.replace(line, "$escape$", "$X"+escapeToken+"$"))
finally:
	fIn.close()
	fOut.close()

