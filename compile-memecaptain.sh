mksquashfs -version

curl -L https://gw.alipayobjects.com/os/enclose-prod/1fd23e6b-d48f-4ed0-94dd-f0f539960253/rubyc-v0.4.0-linux-x64.gz | gunzip > rubyc
chmod +x rubyc

export CPPFLAGS="-P"
./rubyc --gem=meme_captain --gem-version=0.3.1 --output=resources/memecaptain/memecaptain bundle memecaptain
