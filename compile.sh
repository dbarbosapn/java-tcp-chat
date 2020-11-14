binDir="./bin/"
srcDir="./src/"

mkdir -p $binDir $srcDir

## Javac options
javacOpt="-d $binDir"

javac $javacOpt $srcDir*
