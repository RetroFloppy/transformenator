@rem
@rem Batch file Invoker for Transformenator utilities - call with no parameters for usage instructions 
@rem
@rem Set TRANSFORM_HOME to the absolute location of the transformenator.jar file.
@rem The default location is the current working directory.
@rem

@if "%TRANSFORM_HOME%" == "" goto local
@goto next
:local
@set TRANSFORM_HOME="."

:next
@if "%1" == "" goto usage
@if "%9" == "" goto eight
@java -cp %TRANSFORM_HOME%\transformenator.jar org.transformenator.util."%1" "%2" "%3" "%4" "%5" "%6" "%7" "%8" "%9"
@goto end
:eight
@if "%8" == "" goto seven
@java -cp %TRANSFORM_HOME%\transformenator.jar org.transformenator.util."%1" "%2" "%3" "%4" "%5" "%6" "%7" "%8"
@goto end
:seven
@if "%7" == "" goto six
@java -cp %TRANSFORM_HOME%\transformenator.jar org.transformenator.util."%1" "%2" "%3" "%4" "%5" "%6" "%7"
@goto end
:six
@if "%6" == "" goto five
@java -cp %TRANSFORM_HOME%\transformenator.jar org.transformenator.util."%1" "%2" "%3" "%4" "%5" "%6"
@goto end
:five
@if "%5" == "" goto four
@java -cp %TRANSFORM_HOME%\transformenator.jar org.transformenator.util."%1" "%2" "%3" "%4" "%5"
@goto end
:four
@if "%4" == "" goto three
@java -cp %TRANSFORM_HOME%\transformenator.jar org.transformenator.util."%1" "%2" "%3" "%4"
@goto end
:three
@if "%3" == "" goto two
@java -cp %TRANSFORM_HOME%\transformenator.jar org.transformenator.util."%1" "%2" "%3"
@goto end
:two
@if "%2" == "" goto one
@java -cp %TRANSFORM_HOME%\transformenator.jar org.transformenator.util."%1" "%2"
@goto end
:one
@java -cp %TRANSFORM_HOME%\transformenator.jar org.transformenator.util."%1"
@goto end

:usage
@java -cp %TRANSFORM_HOME%\transformenator.jar org.transformenator.util.TransformUtilities

:end