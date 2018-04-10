@rem
@rem Invoker for Transform - call with no parameters for usage instructions 
@rem
@rem Set TRANSFORM_HOME to the absolute location of the transformenator.jar file.  The default
@rem location is the current working directory.
@rem

@if "%TRANSFORM_HOME%" == "" goto local
@goto next
:local
@set TRANSFORM_HOME="."

:next
@java -cp %TRANSFORM_HOME%\transformenator.jar org.transformenator.Transform %*

:end
