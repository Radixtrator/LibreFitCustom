import json
import sys

with open('./app/src/main/res/raw/exercises.json', 'r') as f:
    exercises = json.load(f)

# Search for specific exercises we need
search_terms = {
    'Deadlift': ['Barbell Deadlift', 'Deadlift', 'Sumo Deadlift', 'Romanian Deadlift'],
    'Pullup': ['Pullup', 'Pull-Up', 'Weighted Pull'],
    'Hammer': ['Hammer Curl'],
    'Face Pull': ['Face Pull'],
    'Cable Row': ['Cable Row', 'Seated Cable Row'],
    'Curl': ['Barbell Curl', 'Dumbbell Curl', 'Bicep Curl'],
    'Bench Press': ['Barbell Bench Press', 'Dumbbell Bench Press', 'Decline Dumbbell Bench'],
    'Press': ['Shoulder Press', 'Dumbbell Press', 'OHP'],
    'Squat': ['Barbell Squat', 'Front Squat', 'Squat'],
    'Row': ['Barbell Row', 'Bent Over Row', 'Dumbbell Row'],
    'Leg Curl': ['Leg Curl', 'Seated Leg Curl'],
    'Calf Raise': ['Calf Raise', 'Calf Press'],
    'Tricep': ['Tricep Pushdown', 'Tricep Extension', 'Triceps']
}

# Also do a simpler search
for ex in exercises:
    name = ex.get('name', '')
    ex_id = ex.get('id', '')
    
    if any(keyword in name for keyword in ['Deadlift', 'Pullup', 'Hammer', 'Face Pull', 'Cable Row', 'Dumbbell Curl', 'Bench Press', 'Shoulder Press', 'Squat', 'Leg Press', 'Leg Curl', 'Calf Raise', 'Romanian', 'Tricep Pushdown', 'Tricep Extension', 'Bent Over', 'Side Lateral', 'Front Raise']):
        print(f'"{name}" -> {ex_id}')
