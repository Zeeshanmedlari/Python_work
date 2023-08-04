class dog:

    def __init__(self, name, age):
        self.name = name
        self.age = age

    def sit(self):
        print(f"{self.name} is now sitting")

    def rollover(self):
        print(f"{self.name} is now Rolling")