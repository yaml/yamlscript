import sys, json, yaml

with open(sys.argv[1], 'r') as file:
    try:
        data = yaml.safe_load(file.read())

    except yaml.YAMLError as exc:
        print(exc)

print(json.dumps(data, indent=2))

