# Oapimerge

Simple command line utility for merging multiple open api schemas into one.

# Usage

```
oapimerge [options]

  -o, --out file <file>  Specify output file for merged schema. By default - output.yaml in current working directory. Possible file extensions: yaml, json
  -s, --schema <schema>  Source openapi schema file
  --help                 prints this usage text
```

Or run this tool as docker image: https://hub.docker.com/r/lodik/oapimerge

```
docker run --rm -w /openapi -v `pwd`:/openapi lodik/oapimerge:latest [options]
```