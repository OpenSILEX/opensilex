import json
import random

# High mountains list
high_mountains = [
    "Everest", "K2", "Kangchenjunga", "Lhotse",
    "Makalu", "Cho Oyu", "Dhaulagiri", "Manaslu",
    "Nanga Parbat", "Annapurna", "Gasherbrum", "Broad Peak",
    "Shishapangma", "Makalu II"
]

# Generate a JSON document following the specified rules
def generate_document(index):
    type_index = index % 10
    document = {
        "uri": f"opensilex:uri_{index}",
        "rdfType": f"opensilex:type_{type_index}",
        "publicationDate": "2023-11-30T12:00:00Z",
        "lastUpdateDate": "2023-11-30T12:00:00Z",
        "string": high_mountains[index % len(high_mountains)],
        "stringList": random.sample(high_mountains, 3),
        "integer": random.randint(1, 1000),
        "integerList": random.sample(range(1, 1001), 3),
        "nestedMongoTestModel": {
            "uri": f"opensilex:uri_{index}_1",
            "rdfType": f"opensilex:type_{type_index}",
            "publicationDate": "2023-11-30T12:00:00Z",
            "lastUpdateDate": "2023-11-30T12:00:00Z",
            "string": high_mountains[(index + 1) % len(high_mountains)],
            "stringList": random.sample(high_mountains, 3),
            "integer": random.randint(1, 1000),
            "integerList": random.sample(range(1, 1001), 3)
        }
    }
    return document

# Generate JSON documents
max = 100
json_documents = [generate_document(i) for i in range(1, max+1)]

# Save generated JSON documents to a file
with open("generated_documents.json", "w") as file:
    json.dump(json_documents, file, indent=2)
