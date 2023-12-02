$ScriptPath = $MyInvocation.MyCommand.Path
$ProjectDir = Resolve-Path $ScriptPath\..\..
$JAppJar = "$ProjectDir\build\japp.jar"

if(-Not(Test-Path -Path $JAppJar))
{
    throw "Please build the project using '.\gradlew' first"
}

java -jar $JAppJar @args
exit $LASTEXITCODE
