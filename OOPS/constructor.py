class cars(object):
    '''This is just a doc_string'''

    def __init__(self, carbrand):
        print("The car brand is", carbrand)

    def carDetails(self, carvariant, carBase):
        print("Car variant is", carvariant)
        print("Car Base is ", carBase)

objCar= cars("Audi")
objCar.carDetails("v5", "BaseModel")
objCar1= cars("BMW")
objCar1.carDetails("c5", "TopEnd")

print("=========Second Car===========")
