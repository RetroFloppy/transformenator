@rem Usage: transformdirectory.bat transform origindir destdir
@rem Set TRANSFORM_HOME to the location of the transformenator.jar file.
@for /f "usebackq delims=|" %%f in (`dir /b "%2"`) do java -jar %TRANSFORM_HOME%\transformenator.jar %1 "%2\%%f" "%3\%%f.rtf"