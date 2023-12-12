import json
import random
from typing import List

# High mountains list
plants = [
    "Apple",
    "Banana",
    "Orange",
    "Strawberry",
    "Grapes",
    "Watermelon",
    "Pineapple",
    "Avocado",
    "Tomato",
    "Carrot",
    "Broccoli",
    "Spinach",
    "Potato",
    "Onion",
    "Cucumber",
    "Corn",
    "Rice",
    "Wheat",
    "Oats",
    "Barley"
]

colors = [
    "Red",
    "Blue",
    "Green",
    "Yellow",
    "Purple",
    "Orange",
    "Pink",
    "Brown",
    "Black",
    "White",
    "Gray",
    "Teal",
    "Lavender",
    "Cyan",
    "Magenta",
    "Maroon",
    "Navy",
    "Olive",
    "Turquoise",
    "Gold"
]


# Generate a JSON document following the specified rules
def generate_document(index, documents: List[any]):
    type_index = index % 10

    nested = None
    nested_list = []
    if documents:
        nested = random.choice(documents)
        nested_list = random.sample(documents, 3)

    document = {
        "uri": f"opensilex:{index}",
        "rdfType": f"opensilex:type_{type_index}",
        "publicationDate": "2023-11-30T12:00:00Z",
        "lastUpdateDate": "2023-11-30T12:00:00Z",
        "name": random.choice(plants),
        "tags": random.sample(colors, 3),
        "id": index,
        "values": random.sample(range(1, 10000), 5),
        "nested": nested,
        "nestedList": nested_list
    }
    return document


# Generate JSON documents
max = 100
nested_documents = [generate_document(i, []) for i in range(max + 1, (max * 2) + 1)]
json_documents = [generate_document(i, nested_documents) for i in range(1, max + 1)]

# Save generated JSON documents to a file
with open("generated_documents.json", "w") as file:
    json.dump(json_documents, file, indent=2)
