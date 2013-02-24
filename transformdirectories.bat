@rem Usage: transformdirectories.bat origindir destdir transform
for /f "usebackq delims=|" %%f in (`dir /b "%1"`) do java -jar transformenator.jar -t %3 < "%1\%%f" > "%2\%%f.rtf"