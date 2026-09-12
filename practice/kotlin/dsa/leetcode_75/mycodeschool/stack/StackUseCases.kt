package dsa.leetcode_75.mycodeschool.stack


fun isBalancedParenthesis(str: String): Boolean {
    val parenthesisPair = hashMapOf(
        '(' to ')',
        '[' to ']',
        '{' to '}'
    )

    val closingParenthesis = parenthesisPair.values
    val openingParenthesis = parenthesisPair.keys

    val stack = StackArray<Char>(str.length)

    for (chr in str) {
        if (openingParenthesis.contains(chr)) {
            stack.push(chr)
        } else if (closingParenthesis.contains(chr)) {
            val top = stack.top()
            if (parenthesisPair[top] != chr) {
                return false
            }
            stack.pop()
        }
    }

    return stack.isEmpty()
}

var operatorPrecedenceMap = hashMapOf<String, Int>(
    "+" to 1,
    "-" to 1,
    "*" to 2,
    "/" to 2
)

var OPEN_PAREN = "("
var CLOSE_PAREN = ")"

fun comparePrecedence(op: String, op2: String): Boolean {
    return (operatorPrecedenceMap.getOrDefault(op, 0) > operatorPrecedenceMap.getOrDefault(op2, 0))
}

fun infixToPostfix(tokens: Array<String>): List<String> {
    var postFix = mutableListOf<String>();

    var stack = StackArray<String>(tokens.size)

    for (str in tokens) {
        if (str == OPEN_PAREN) {
            stack.push(str)
        } else if (str == CLOSE_PAREN) {
            while (!stack.isEmpty() && stack.top() != OPEN_PAREN) {
                postFix.add(stack.pop())
            }
            stack.pop()
        } else if (operatorPrecedenceMap.contains(str)) {
            while (!stack.isEmpty() && stack.top() != OPEN_PAREN && !comparePrecedence(str, stack.top())) {
                postFix.add(stack.pop())
            }
            stack.push(str)
        } else {
            postFix.add(str)
        }
    }

    while (!stack.isEmpty()) {
        postFix.add(stack.pop())
    }

    return postFix
}


fun applyOperation(a: String, b: String, op: String): Int {
    var aVal = a.toInt()
    var bVal = b.toInt()

    return when (op) {
        "+" -> aVal + bVal
        "-" -> aVal - bVal
        "*" -> aVal * bVal
        "/" -> aVal / bVal

        else -> {0}
    }
}

fun evalPostFix(tokens: List<String>): Int {
    var stack = StackArray<String>()

    for (chr in tokens) {
        if (operatorPrecedenceMap.contains(chr)) {
            var b = stack.pop()
            var a = stack.pop()

            stack.push(""+applyOperation(a, b , chr))
        } else {
            stack.push(chr)
        }
    }

    return stack.pop().toInt()
}

fun evalInfix(tokens: Array<String>): Int {
    var postFix = infixToPostfix(tokens)

    return evalPostFix(postFix)
}

fun main() {
//    println(isBalancedParenthesis("([]])"))

    print(evalInfix(arrayOf("6", "*", "(", "4", "-", "2", ")")))
}

