package il.ac.technion.cs.softwaredesign

import java.util.Stack


import java.io.IOException
import java.util.ArrayList

import java.util.Scanner
import java.util.StringTokenizer
import java.io.File



class Expression
/**
 * Initializes this Expression object with an input expression. Sets all other
 * fields to null.
 *
 * @param expr Expression
 */
(
        /**
         * Expression to be evaluated
         */
        internal var expr: String) {

    /**
     * Scalar symbols in the expression
     */
    internal var scalars: ArrayList<ScalarSymbol>? = null

    /**
     * Array symbols in the expression
     */
    internal var arrays: ArrayList<ArraySymbol>? = null

    /**
     * Positions of opening brackets
     */
    internal var openingBracketIndex: ArrayList<Int>? = null

    /**
     * Positions of closing brackets
     */
    internal var closingBracketIndex: ArrayList<Int>? = null

    /**
     * Matches parentheses and square brackets. Populates the openingBracketIndex and
     * closingBracketIndex array lists in such a way that closingBracketIndex[i] is
     * the position of the bracket in the expression that closes an opening bracket
     * at position openingBracketIndex[i]. For example, if the expression is:
     * <pre>
     * (a+(b-c))*(d+A[4])
    </pre> *
     * then the method would return true, and the array lists would be set to:
     * <pre>
     * openingBracketIndex: [0 3 10 14]
     * closingBracketIndex: [8 7 17 16]
    </pre> *
     *
     * @return True if brackets are matched correctly, false if not
     */
    // COMPLETE THIS METHOD
    // FOLLOWING LINE ADDED TO MAKE COMPILER HAPPY
    val isLegallyMatched: Boolean
        get() {
            openingBracketIndex = ArrayList()
            closingBracketIndex = ArrayList()
            val brackets = Stack<Bracket>()
            val close = Stack<Bracket>()
            for (i in 0 until expr.length) {
                if (expr[i] == '(' || expr[i] == '[') {
                    brackets.push(Bracket(expr[i], i))
                    openingBracketIndex!!.add(i)
                } else if (expr[i] == ')') {
                    if (brackets.isEmpty())
                        return false
                    if (brackets.peek().ch === '(') {
                        brackets.pop()
                        if (brackets.isEmpty()) {
                            closingBracketIndex!!.add(i)
                            while (!close.isEmpty()) {
                                closingBracketIndex!!.add(close.pop().pos)
                            }
                        } else
                            close.push(Bracket(expr[i], i))
                    } else
                        return false
                } else if (expr[i] == ']') {
                    if (brackets.isEmpty())
                        return false
                    if (brackets.peek().ch === '[') {
                        brackets.pop()
                        if (brackets.isEmpty()) {
                            closingBracketIndex!!.add(i)
                            while (!close.isEmpty()) {
                                closingBracketIndex!!.add(close.pop().pos)
                            }
                        } else
                            close.push(Bracket(expr[i], i))
                    } else
                        return false
                } else
                    continue
            }

            return brackets.isEmpty()

        }

    init {
        scalars = null
        arrays = null
        openingBracketIndex = null
        closingBracketIndex = null
    }

    /**
     * Populates the scalars and arrays lists with symbols for scalar and array
     * variables in the expression. For every variable, a SINGLE symbol is created and stored,
     * even if it appears more than once in the expression.
     * At this time, values for all variables are set to
     * zero - they will be loaded from a file in the loadSymbolValues method.
     */
    fun buildSymbols() {
        arrays = ArrayList<ArraySymbol>()
        scalars = ArrayList<ScalarSymbol>()

        var temp = ""
        for (i in 0 until expr.length) {
            temp = temp + expr[i]
            if (expr[i] == '[') {
                temp = "$temp~"
            }
        }
        val str = StringTokenizer(temp, " \t*+-/()]~")

        while (str.hasMoreElements()) {
            val x = str.nextToken()
            if (x[x.length - 1] == '[') {
                arrays!!.add(ArraySymbol(x.substring(0, x.length - 1)))
            } else {
                if (!Character.isLetter(x[0]))
                    continue
                else
                    scalars!!.add(ScalarSymbol(x))
            }

        }


    }


    /**
     * Loads values for symbols in the expression
     *
     * @param sc Scanner for values input
     * @throws IOException If there is a problem with the input
     */
    @Throws(IOException::class)
    fun loadSymbolValues(sc: Scanner) {
        while (sc.hasNextLine()) {
            val st = StringTokenizer(sc.nextLine().trim { it <= ' ' })
            val numTokens = st.countTokens()
            val sym = st.nextToken()
            val ssymbol = ScalarSymbol(sym)
            var asymbol = ArraySymbol(sym)
            val ssi = scalars!!.indexOf(ssymbol)
            val asi = arrays!!.indexOf(asymbol)
            if (ssi == -1 && asi == -1) {
                continue
            }
            val num = Integer.parseInt(st.nextToken())
            if (numTokens == 2) { // scalar symbol
                scalars!![ssi].value = num
            } else { // array symbol
                asymbol = arrays!![asi]
                asymbol.values = IntArray(num)
                // following are (index,val) pairs
                while (st.hasMoreTokens()) {
                    val tok = st.nextToken()
                    val stt = StringTokenizer(tok, " (,)")
                    val index = Integer.parseInt(stt.nextToken())
                    val `val` = Integer.parseInt(stt.nextToken())
                    asymbol.values!![index] = `val`
                }
            }
        }
    }

    /**
     * Evaluates the expression, using RECURSION to evaluate subexpressions and to evaluate array
     * subscript expressions.
     *
     * @return Result of evaluation
     */
    fun evaluate(): Float {

        val last = evaluate_recur(expr)

        return java.lang.Float.parseFloat(last)
    }

    //Recursive Solution
    private fun evaluate_recur(expression: String): String {
        for (i in 0 until expression.length) {

            if (expression[i] == '(' && i != expression.length - 1)
            //handles parenthesis
            {
                val before = expression.substring(0, i)
                val after = expression.substring(findEndIndex(expression)!! + 1, expression.length)
                val s = evaluate_recur(expression.substring(i + 1, findEndIndex(expression)!!))//recursive call
                val output = before + s + after
                return if (after.trim { it <= ' ' }.length > 1)
                    evaluate_recur("" + output) //recursive call
                else
                    "" + baseEvaluate(output) // evaluates expression using numbers


            }
            if (expression[i] == '[' && i != expression.length - 1)
            //handles square brackets
            {
                val before = expression.substring(0, i + 1)
                val after = expression.substring(findEndIndex(expression)!!, expression.length)
                val s = evaluate_recur(expression.substring(i + 1, findEndIndex(expression)!!)) //recursive call
                val output = before + s + after
                return if (after.trim { it <= ' ' }.length > 1)
                    evaluate_recur(Math.floor(baseEvaluate(before + s + after[0]).toDouble()).toString() + after.substring(1)) //recursive call
                else
                    "" + Math.floor(baseEvaluate(output).toDouble()) // evaluates expression using numbers
            }

        }
        return "" + baseEvaluate(expression)
    }

    private fun findEndIndex(expression: String): Int? {
        val opening = getOpeningBracketIndexList(expression)
        val closing = getClosingBracketIndexList(expression)

        return closing[0]
    }


    private fun baseEvaluate(expression: String) //converts expressions that have variables into an expression with just numbers
            : Float {
        var translated = ""
        var i = 0
        while (i < expression.trim { it <= ' ' }.length) {
            var temp = ""
            if (expression[i] == '.') {
                translated += expression[i]
                i++
                continue
            }
            if (expression[i] == ')' || expression[i] == '(') {
                i++
                continue
            }
            if (Character.isLetter(expression[i])) {
                var loop = true
                while (loop) {
                    temp = temp + expression[i]
                    if (i != expression.length - 1 && Character.isLetter(expression[i + 1])) {
                        loop = true
                    } else {
                        break
                    }
                    i++
                }


                var found = false
                var index = 0
                var scale: ScalarSymbol
                while (!found && index < scalars!!.size) {
                    scale = scalars!![index]
                    if (temp == scale.name) {
                        translated = translated + scale.value
                        found = true
                    } else
                        index++
                }

                var found2 = false
                var index2 = 0
                var j = 0
                var arr: ArraySymbol
                while (!found2 && index2 < arrays!!.size) {
                    arr = arrays!![index2]
                    if (temp == arr.name) {
                        var ind = ""
                        var a = i + 1
                        while (a < expression.length) {
                            if (Character.isDigit(expression[a])) {
                                ind = "" + expression[a]
                                break
                            }
                            a++
                        }
                        j = a
                        val indy = Integer.parseInt(ind)
                        translated = translated + arr.values!![indy]
                        found2 = true
                    } else
                        index2++
                }
                if (found2) {
                    while (j < expression.length) {
                        if (expression[j] == ']') {
                            i = j
                            break
                        }
                        j++

                    }
                }
            } else
                translated = translated + expression[i]
            i++
        }


        return postfixToSolution(infixToPostfix(translated))


    }

    private fun infixToPostfix(str: String) //converts expression of numbers from infix to postfix notation using stacks
            : String {
        val postfix = Stack<String>()
        var pst = ""
        var i = 0


        while (i < str.length) {
            var op = ""
            op = op + str[i]
            if (Character.isDigit(str[i]) || str[i] == '.') {
                var loop = true
                while (loop) {
                    pst = pst + str[i]
                    if (i != str.length - 1 && (Character.isDigit(str[i + 1]) || str[i + 1] == '.')) {
                        i++
                        continue
                    } else {
                        pst = "$pst~"
                        loop = false
                    }
                }
                i++
                continue
            } else if (str[i] == ' ') {
                i++
                continue
            } else {
                if (postfix.isEmpty()) {
                    postfix.push(op)
                } else {
                    var loop = true
                    while (loop) {
                        if (postfix.isEmpty() == true) {
                            postfix.push(op)
                            break
                        }

                        val topStackValue = postfix.peek()
                        val scannedChar = str[i]
                        var higherPrecedence: Boolean
                        //handles precedence of operators
                        higherPrecedence = if ((topStackValue == "*" || topStackValue == "/") && (scannedChar == '+' || scannedChar == '-'))
                            true
                        else if ((topStackValue == "+" || topStackValue == "-") && (scannedChar == '+' || scannedChar == '-'))
                            true
                        else (topStackValue == "*" || topStackValue == "/") && (scannedChar == '*' || scannedChar == '/')

                        if (higherPrecedence) {
                            val x = postfix.pop()
                            pst = "$pst$x~"

                        } else {
                            postfix.push("" + scannedChar)
                            loop = false
                        }
                    }
                }
            }
            i++
        } // }
        while (!postfix.isEmpty()) {
            pst = pst + postfix.peek() + "~"
            postfix.pop()
        }

        return pst
    }


    private fun postfixToSolution(str: String) //converts postfix notation to a value
            : Float {
        val operand = Stack<Float>()
        val operand_excep = Stack<Char>()
        val str_token = StringTokenizer(str, "~")

        while (str_token.hasMoreElements()) {
            val op = str_token.nextToken()

            if (Character.isDigit(op[0])) {
                val x = java.lang.Float.parseFloat(op)
                operand.push(x)
            } else {
                if (operand.size == 1) {
                    if (op[0] == '-') {
                        val operand1 = operand.peek()
                        operand.pop()
                        val operand2 = 0.toFloat()
                        val operator = op[0]
                        val retValue = findValue(operand1, operand2, operator)
                        operand.push(retValue)
                    } else {
                        operand_excep.push(op[0])
                    }
                } else if (!operand_excep.isEmpty()) {
                    val operator = operand_excep.pop()
                    val operand1 = operand.peek()
                    operand.pop()
                    val operand2 = operand.peek()
                    val retValue = findValue(operand1, operand2, operator)
                    operand.pop()
                    if (op[0] == '-')
                        operand.push(0 - retValue)
                    else
                        operand.push(0 - retValue)
                } else {
                    val operand1 = operand.peek()
                    operand.pop()
                    val operand2 = operand.peek()
                    val operator = op[0]
                    val retValue = findValue(operand1, operand2, operator)
                    operand.pop()
                    operand.push(retValue)
                }
            }
        }

        return operand.peek()
    }

    private fun findValue(a: Float?, b: Float?, x: Char): Float {
        return if (x == '+')
            a!! + b!!
        else if (x == '-')
            b!! - a!!
        else if (x == '*')
            a!! * b!!
        else
            b!! / a!!
    }

    private fun getOpeningBracketIndexList(expression: String): ArrayList<Int> { //utility method
        val openingIndex = ArrayList<Int>()
        val closingIndex = ArrayList<Int>()
        // COMPLETE THIS METHOD
        val brackets = Stack<Bracket>()
        val close = Stack<Bracket>()
        for (i in 0 until expression.length) {
            if (expression[i] == '(' || expression[i] == '[') {
                brackets.push(Bracket(expression[i], i))
                openingIndex.add(i)
            } else if (expression[i] == ')') {

                if (brackets.peek().ch === '(') {
                    brackets.pop()
                    if (brackets.isEmpty()) {
                        closingIndex.add(i)
                        while (!close.isEmpty()) {
                            closingIndex.add(close.pop().pos)
                        }
                    } else
                        close.push(Bracket(expression[i], i))
                }

            } else if (expression[i] == ']') {

                if (brackets.peek().ch === '[') {
                    brackets.pop()
                    if (brackets.isEmpty()) {
                        closingIndex.add(i)
                        while (!close.isEmpty()) {
                            closingIndex.add(close.pop().pos)
                        }
                    } else
                        close.push(Bracket(expression[i], i))
                }

            } else
                continue
        }

        return openingIndex
        // FOLLOWING LINE ADDED TO MAKE COMPILER HAPPY

    }


    private fun getClosingBracketIndexList(expression: String): ArrayList<Int> { //utility method
        val openingIndex = ArrayList<Int>()
        val closingIndex = ArrayList<Int>()
        // COMPLETE THIS METHOD
        val brackets = Stack<Bracket>()
        val close = Stack<Bracket>()
        for (i in 0 until expression.length) {
            if (expression[i] == '(' || expression[i] == '[') {
                brackets.push(Bracket(expression[i], i))
                openingIndex.add(i)
            } else if (expression[i] == ')') {

                if (brackets.peek().ch === '(') {
                    brackets.pop()
                    if (brackets.isEmpty()) {
                        closingIndex.add(i)
                        while (!close.isEmpty()) {
                            closingIndex.add(close.pop().pos)
                        }
                    } else
                        close.push(Bracket(expression[i], i))
                }

            } else if (expression[i] == ']') {

                if (brackets.peek().ch === '[') {
                    brackets.pop()
                    if (brackets.isEmpty()) {
                        closingIndex.add(i)
                        while (!close.isEmpty()) {
                            closingIndex.add(close.pop().pos)
                        }
                    } else
                        close.push(Bracket(expression[i], i))
                }

            } else
                continue
        }

        return closingIndex
        // FOLLOWING LINE ADDED TO MAKE COMPILER HAPPY

    }


    fun printScalars() {
        for (ss in scalars!!) {
            System.out.println(ss)
        }
    }

    /**
     * Utility method, prints the symbols in the arrays list
     */
    fun printArrays() {
        for (`as` in arrays!!) {
            System.out.println(`as`)
        }
    }

    companion object {

        /**
         * String containing all delimiters (characters other than variables and constants),
         * to be used with StringTokenizer
         */
        val delims = " \t*+-/()[]"
    }

}

/**
 * This class encapsulates a (name, integer value) pair for a scalar (non-array) variable.
 * The variable name is a sequence of one or more letters.
 *
 * @author ru-nb-cs112
 */
class ScalarSymbol
/**
 * Initializes this symbol with given name, and zero value
 *
 * @param name Variable name
 */
(
        /**
         * Name, sequence of letters
         */
        var name: String) {

    /**
     * Integer value
     */
    var value: Int = 0

    init {
        value = 0
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
    override fun toString(): String {
        return "$name=$value"
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is ScalarSymbol) {
            return false
        }
        val ss = other as ScalarSymbol?
        return name == ss!!.name
    }
}

/**
 * This class encapsulates a (name, array of integer values) pair for an array variable.
 * The name is a sequence of one or more letters.
 *
 * @author ru-nb-cs112
 */
class ArraySymbol
/**
 * Initializes this symbol with given name, and sets values to null.
 *
 * @param name Name of array
 */
(
        /**
         * Name, sequence of letters
         */
        var name: String) {

    /**
     * Array of integer values
     */
    var values: IntArray? = null

    init {
        values = null
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
    override fun toString(): String {
        if (values == null || values!!.isEmpty()) {
            return "$name=[ ]"
        }
        val sb = StringBuilder()
        sb.append(name)
        sb.append("=[")
        sb.append(values!![0])
        for (i in 1 until values!!.size) {
            sb.append(',')
            sb.append(values!![i])
        }
        sb.append(']')
        return sb.toString()
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is ArraySymbol) {
            return false
        }
        val `as` = other as ArraySymbol?
        return name == `as`!!.name
    }
}

/**
 * This class enacapsulates a parenthesis ('(' or ')') or square bracket ('[' or ']'), and
 * its position in the expression.
 *
 * @author run-nb-cs112
 */
class Bracket
/**
 * Initializes this bracket to given char and position
 *
 * @param ch Bracket character
 * @param pos Position in expression
 */
(
        /**
         * Paren or square bracket
         */
        var ch: Char,
        /**
         * Position at which bracket occurs in expression
         */
        var pos: Int)

object Evaluator {

    /**
     * @param args
     */
    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val scstdin = Scanner(System.`in`)
        while (true) {
            print("\nEnter the expression, or hit return to quit => ")
            var line = scstdin.nextLine()
            if (line.isEmpty()) {
                return
            }
            val expr = Expression(line)
            val match = expr.isLegallyMatched
            println("Expression legally matched: $match")
            if (!match) {
                continue
            }
            expr.buildSymbols()

            print("Enter symbol values file name, or hit return if no symbols => ")
            line = scstdin.nextLine()
            if (line.isNotEmpty()) {
                val scfile = Scanner(File(line))
                expr.loadSymbolValues(scfile)
                expr.printScalars()
                expr.printArrays()
            }
            println("Value of expression = " + expr.evaluate())
        }

    }

}