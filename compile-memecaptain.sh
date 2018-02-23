mksquashfs -version

curl -L http://enclose.io/rubyc/rubyc-linux-x64.gz | gunzip > rubyc
chmod +x rubyc

export CPPFLAGS="-P"
./rubyc --gem=meme_captain --gem-version=0.3.1 --output=resources/memecaptain/memecaptain bundle memecaptain
