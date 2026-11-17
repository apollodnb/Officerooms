"""0.38.0: Underside Key needs the Ticket Stub of the cinema; new milestone advancements for the quests."""
import json
import os

D = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'data', 'office_rooms')
O = 'office_rooms:'
KEY = ['chlorine_pearl', 'faded_photograph', 'thread_of_the_maze', 'room_key_000', 'humming_fuse', 'party_smile_badge', 'ticket_stub']


def w(path, obj):
    path = os.path.join(D, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=1)


w('recipes/underside_key.json', {"type": "minecraft:crafting_shapeless", "category": "misc",
                                 "ingredients": [{"item": O + a} for a in KEY], "result": {"item": O + "underside_key"}})
for m in ('slide_ride', 'cinema_top', 'hotel_top'):
    w(f'advancements/milestones/{m}.json', {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
print('ok')
