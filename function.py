'''def addTwodigits (x,y):
    print(x*y)

addTwodigits(30,40)

def multiplyDigits (a,b):
    print(a*b)

multiplyDigits(70,80)'''


from unittest import result


def recFunction(n):
    if (n > 4):
        res=n + recFunction(n-1)
    else:
        res = 0
    return res
print("Recursion")
print(recFunction(5))


def studentsNames(*names):
    print(names)

studentsNames("Zeeshan Ali", "Ruksar", 54, 76, ...)


def zeeshanData(**Data):
    print(Data)

zeeshanData(name="Zeeshan", age=30, state= "karnataka")


def allArgParams(name,*languages,country="USA",**books):
    print(f"my name is {name} and i live in {country} and i know these many{languages}\
          and i learned it from these {books}")

allArgParams("Zeeshan","spanish","Hindi","Kannada","English",book1="Author1",\
              book2="Author2", book3="author3")


def greetings():
    print("Hello")

greetings()

def greet_user(username):
    print(f"Hello {username.title()} How you Doing")

greet_user("zeeshan ali")


def describe_pet(animal_type, pet_name="Everything"):
    print(f"I have an {animal_type.upper()}")
    print(f"I have an {animal_type.lower()} and its name is {pet_name.title()}")

#describe_pet("Horse", "Jordan")
#describe_pet("dog", "blacky")
#describe_pet(animal_type="cat", pet_name="sweety")
describe_pet("Anything")

# Returning the values

def fullNames(firstName, secondName):
    fullName= f"my first name is {firstName} and my last name is {secondName}"
    return fullName.title()

musician=fullNames("zeeshan", "ali")
print(musician)


'''def callerFunction(firstName, lastName, secondName="ali"):
    callerName = f"my first name is {firstName} and middle name is {secondName} and my last name is {lastName}"
    return callerName.title()

iDontKnow = callerFunction("zeeshan", "medlari")
iDontKnow = callerFunction("Dawood", "Khan")
print(iDontKnow)'''

# Making an argument optional

'''def getFormattedNames(firstName,lastName,middleName=""):
    if middleName:
        fullName = f"{firstName} {middleName} {lastName}"
    else:
        fullName = f"{firstName} {lastName}"
    return fullName.title()

music = getFormattedNames("zeeshan", "ali", "medlari")
print(music)

music = getFormattedNames("zeeshan", "ali")
print(music)'''

# working with while loop

def inputFunction(firstNname, lastNname):
    nameFull = f"please print my {firstNname} and {lastNname}"
    return nameFull.title()

while True:
    print("Please enter your name")
    print("(please enter q to quit at anytime)")

    fName = input("Enter your first name : _")
    if fName == "q":
        break

    lName = input("Enter your last name : _")
    if lName == "q":
        break

    FullyName = inputFunction("fname", "lname")
    print(f" Hello {FullyName}!")

