@rem
@rem Batch file Invoker for TransformDirectory - call with no parameters for usage instructions 
@rem
@rem Set TRANSFORM_HOME to the location of the transformenator.jar file.  The default
@rem location is the current working directory otherwise.
@rem

@if "%TRANSFORM_HOME%" == "" goto local
@goto next
:local
@set TRANSFORM_HOME="."

:next
@if "%3" == "" goto usage
@if "%4" == "" goto nosuffix

@java -cp %TRANSFORM_HOME%\transformenator.jar org.transformenator.TransformDirectory "%1" "%2" "%3" "%4"
@goto end

:nosuffix
@java -cp %TRANSFORM_HOME%\transformenator.jar org.transformenator.TransformDirectory "%1" "%2" "%3"
@goto end

:usage
@java -cp %TRANSFORM_HOME%\transformenator.jar org.transformenator.TransformDirectory

:end
