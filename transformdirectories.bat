@rem Usage: transformdirectories.bat transform origindir destdir
for /f "usebackq delims=|" %%f in (`dir /b "%2"`) do java -jar transformenator.jar -t %1 < "%2\%%f" > "%3\%%f.txt"