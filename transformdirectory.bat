@rem
@rem Usage: transformdirectory.bat transform origindir destdir [suffix]
@rem Set TRANSFORM_HOME to the location of the transformenator.jar file.
@rem

@if "%3" == "" goto usage
@if "%4" == "" goto nosuffix

@rem
@rem Add a suffix to all transformations performed
@rem
@for /f "usebackq delims=|" %%f in (`dir /b "%2"`) do java -jar %TRANSFORM_HOME%\transformenator.jar %1 "%2\%%f" "%3\%%f.%4"
@goto end

:nosuffix
@rem
@rem Don't add any suffix at all - just put the new file in the new destination with the old name
@rem 
@for /f "usebackq delims=|" %%f in (`dir /b "%2"`) do java -jar %TRANSFORM_HOME%\transformenator.jar %1 "%2\%%f" "%3\%%f"
@goto end

:usage
@echo Usage: transformdirectory.bat transform origindir destdir [suffix]

:end
