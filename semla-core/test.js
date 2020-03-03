
function initial(name) {
    var firstLetter = name.substring(0,1);
    return firstLetter;
}

var test = initial("Christina");

console.log("test: "+test)








class User {
   constructor(name) {
     this.name = name;
   }
}

function computeInitial(aUserToModify) {
    aUserToModify.initial = initial(aUserToModify.name);
}




var user = new User("Christina");
var backupToUserInCaseWeFuckUp = user;

user.isParent = false;

console.log("the name of the user is: " + user.name + " isParent: "+user.isParent)
console.log("the first letter of the user name is: " + initial(user.name))

user = computeInitial(user);



console.log("user is: " + user);
console.log("backupToUserInCaseWeFuckUp is: " + backupToUserInCaseWeFuckUp.initial);




