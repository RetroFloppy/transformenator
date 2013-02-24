@rem Usage: transformdirectory.bat origindir transform
for /f "usebackq delims=|" %%f in (`dir /b "%1"`) do java -jar transformenator.jar -t %2 < "%1\%%f" > "%1\%%f.rtf"