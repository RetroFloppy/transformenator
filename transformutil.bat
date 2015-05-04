@rem
@rem Batch file Invoker for Transformenator utilities - call with no parameters for usage instructions 
@rem
@rem Set TRANSFORM_HOME to the absolute location of the transformenator.jar file.  The default
@rem location is the current working directory.
@rem

@if "%TRANSFORM_HOME%" == "" goto local
@goto next
:local
@set TRANSFORM_HOME="."

:next
@if "%1" == "" goto usage
@java -cp %TRANSFORM_HOME%\transformenator.jar org.transformenator.util."%1" "%2" "%3" "%4" "%5" "%6" "%7" "%8" "%9"
@goto end

:usage
@java -cp %TRANSFORM_HOME%\transformenator.jar org.transformenator.util.TransformUtilities

:end