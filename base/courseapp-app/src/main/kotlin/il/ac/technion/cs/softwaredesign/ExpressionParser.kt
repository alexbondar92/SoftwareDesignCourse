package il.ac.technion.cs.softwaredesign

import java.util.*

// links:
//      https://github.com/gbenroscience/ParserNG
//      https://rosettacode.org/wiki/Parsing/Shunting-yard_algorithm
//      http://www2.lawrence.edu/fast/GREGGJ/CMSC150/071Calculator/Calculator.html
//      https://www.technical-recipes.com/2011/a-mathematical-expression-parser-in-java-and-cpp/            - full java implementation

object ExpressionParser {
    // Associativity constants for operators
    private const val LEFT_ASSOC = 0
    private const val RIGHT_ASSOC = 1

    // Operators
    private val OPERATORS = HashMap<String, IntArray>()

    init {
        // Map<"token", []{precendence, associativity}>
        OPERATORS["+"] = intArrayOf(0, LEFT_ASSOC)
        OPERATORS["-"] = intArrayOf(0, LEFT_ASSOC)
        OPERATORS["*"] = intArrayOf(5, LEFT_ASSOC)
        OPERATORS["/"] = intArrayOf(5, LEFT_ASSOC)
    }

    // Test if token is an operator
    private fun isOperator(token: String): Boolean {
        return OPERATORS.containsKey(token)
    }

    // Test associativity of operator token
    private fun isAssociative(token: String, type: Int): Boolean {
        if (!isOperator(token)) {
            throw IllegalArgumentException("Invalid token: $token")
        }

        return OPERATORS[token]!![1] == type
    }

    // Compare precedence of operators.
    private fun cmpPrecedence(token1: String, token2: String): Int {
        if (!isOperator(token1) || !isOperator(token2)) {
            throw IllegalArgumentException("Invalid tokens: " + token1
                    + " " + token2)
        }
        return OPERATORS[token1]!![0] - OPERATORS[token2]!![0]
    }

    // Convert infix expression format into reverse Polish notation
    fun infixToRPN(inputTokens: Array<String>): Array<String> {
        val out = ArrayList<String>()
        val stack = Stack<String>()

        // For each token
        for (token in inputTokens) {
            // If token is an operator
            if (isOperator(token)) {
                // While stack not empty AND stack top element
                // is an operator
                while (!stack.empty() && isOperator(stack.peek())) {
                    if (isAssociative(token, LEFT_ASSOC) && cmpPrecedence(token, stack.peek()) <= 0 || isAssociative(token, RIGHT_ASSOC) && cmpPrecedence(token, stack.peek()) < 0) {
                        out.add(stack.pop())
                        continue
                    }
                    break
                }
                // Push the new operator on the stack
                stack.push(token)
            } else if (token == "(") {
                stack.push(token)  //
            } else if (token == ")") {
                while (!stack.empty() && stack.peek() != "(") {
                    out.add(stack.pop())
                }
                stack.pop()
            } else {
                out.add(token)
            }// If token is a number
            // If token is a right bracket ')'
            // If token is a left bracket '('
        }
        while (!stack.empty()) {
            out.add(stack.pop())
        }
        val output = arrayOfNulls<String>(out.size)
        return out.toTypedArray()
    }

    fun RPNtoDouble(tokens: Array<String>): Double {
        val stack = Stack<String>()

        // For each token
        for (token in tokens) {
            // If the token is a value push it onto the stack
            if (!isOperator(token)) {
                stack.push(token)
            } else {
                // Token is an operator: pop top two entries
                val d2 = java.lang.Double.valueOf(stack.pop())
                val d1 = java.lang.Double.valueOf(stack.pop())

                //Get the result
                val result = if (token.compareTo("+") == 0)
                    d1 + d2
                else if (token.compareTo("-") == 0)
                    d1 - d2
                else if (token.compareTo("*") == 0)
                    d1 * d2
                else
                    d1 / d2

                // Push result onto stack
                stack.push(result.toString())
            }
        }

        return java.lang.Double.valueOf(stack.pop())
    }
/*
    // Example:
    @JvmStatic
    private fun main(args: Array<String>) {
        println("( 1 + 2 ) * ( 3 / 4 ) - ( 5 + 6 )")
        println("( 1 + 2 ) * ( 3 / 4 ) - ( 5 + 6 )".replace(" ",""))
        println("( 1 + 2 ) * ( 3 / 4 ) - ( 5 + 6 )".replace(" ","").replace(Regex(".(?!$)"), "$0 "))
        val input = "( 1 + 2 ) * ( 3 / 4 ) - ( 5 + 6 )".replace(" ","").replace(Regex(".(?!$)"), "$0 ").split(" ").dropLastWhile { it.isEmpty() }.toTypedArray()
        val output = infixToRPN(input)

        // Build output RPN string minus the commas
        for (token in output) {
            print("$token ")
        }

        // Feed the RPN string to RPNtoDouble to give result
        val result = RPNtoDouble(output)
        println()
        println("result: $result")
    }
    */
}