cars = ("AUDI", "bmw", "suzuki", "toyota")
for car in cars:
    if car.lower() == "audi":
        print(f"Here we have found what we want {car.upper()}")

requested_toppings = "mushrooms"
if requested_toppings != "anchovies":
    print("hold the anchovies")


car = "Zeeshan"
if car != "Tousif":
    print("Here we go")
else:
    print("not found")

answer = 17
if answer != 17:
    print("thats the worng answer")


age = 17
if age > 21:
    print ("Thats correct")


Names = ["Zeeshan", "Tousif", "Dawood", "Nasir"]
"Zeeshan" in Names
print(False)