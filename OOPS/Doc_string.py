from ast import Name


class Classname:
    '''This is just a doc_string to look for a description'''

    def show(self):
        print("This line please")

c1=Classname()
print(Classname.__doc__)
c1.show()



class Person:
    '''This is a doc_string for Person class'''

    Name= "Zeeshan"
    Age= 30
    Country = "India"

    def speak(self):
        print("This person can speak good English")

    def write(self):
        print("The person can write a spanish poem well")

objCreateion = Person()
objCreateion.speak()
objCreateion.write() 

print(objCreateion.Name)