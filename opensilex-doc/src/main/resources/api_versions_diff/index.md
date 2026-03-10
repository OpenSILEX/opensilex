# Run swagger diff

## Download swagger diff 


```bash
wget https://github.com/Sayi/swagger-diff/releases/download/v1.2.2/swagger-diff.jar

```

## Run swagger diff

```bash
java -jar swagger-diff.jar -old swagger_old.json  -new swagger_new.json -v 2.0 -output-mode html > diff.html
# example  
#  java -jar swagger-diff.jar -old swagger1_4.json  -new swagger1_5.json -v 2.0 -output-mode html > diff_1_4_1_5.html

```

Versions are based on swaggerfile with version `{"swagger":"2.0","info":{"version":"1.4.10","title":"OpenSilex API"},....` 

# API differences versions

[1_4 to 1_5](1.4_1.5/diff_1_4_1_5.html)