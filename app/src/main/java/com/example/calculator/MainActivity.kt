package com.example.calculator

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private var negate: Boolean = false;
    private val totalString: StringBuilder = StringBuilder();
    private val currentCmd: StringBuilder = StringBuilder();
    private var seenDot = false;
    private lateinit var textResult: TextView;

    private fun updateText() {
        val sb: StringBuilder = StringBuilder();
        sb.append(totalString);
        if (currentCmd.isNotEmpty()) {
            if (currentCmd[0] in "+-x/") {
                sb.append(currentCmd);
            }
            else {
                if (negate) {
                    sb.append("-");
                }
                sb.append(currentCmd);
            }
        }
        textResult.text = sb.toString();
    }

    private fun evaluateExpression(expr: String): Double? {
        val n = expr.length;
        if (n == 0) {
            return 0.0;
        }
        if (expr[0] in "+x/") {
            return null;
        }
        val pred: HashMap<Char, Int> = hashMapOf(
            'x' to 1,
            '/' to 1,
            '+' to 2,
            '-' to 2
        )

        val postFix = mutableListOf<Any>();
        val opStack = ArrayDeque<Char>();
        var i = 0;
        var j = 0;
        while (i < n) {
            // parse number
            j = i;
            if (expr[j] == '-') {
                j += 1;
            }
            while (j < n && expr[j] in "0123456789.") j += 1
            val num = expr.substring(i, j).toDouble();
            postFix.add(num);

            if (j < n) {
                while (opStack.isNotEmpty() && pred.getOrDefault(opStack.last(), 3) < pred.getOrDefault(expr[j], 3)) {
                    postFix.add(opStack.removeLast());
                }
                opStack.add(expr[j])
                j += 1
            }
            i = j
        }
        while (opStack.isNotEmpty()) {
            postFix.add(opStack.removeLast());
        }
        val numStack = ArrayDeque<Double>();
        for (a in postFix) {
            if (a is Double) {
                numStack.add(a);
            }
            else {
                if (numStack.size < 2) {
                    return null;
                }
                var n2 = numStack.removeLast();
                var n1 = numStack.removeLast();
                if (a == '-') {
                    n1 -= n2;
                }
                else if (a == '+') {
                    n1 += n2;
                }
                else if (a == 'x') {
                    n1 *= n2;
                }
                else {
                    if (n2 == 0.0) {
                        return null;
                    }
                    n1 /= n2;
                }
                numStack.add(n1);
            }
        }
        if (numStack.size != 1) {
            return null;
        }

        return numStack.removeLast();
    }

    private fun finalizeCalculation() {
        val sb: StringBuilder = StringBuilder();
        sb.append(totalString);
        if (currentCmd.isNotEmpty()) {
            if (currentCmd[0] in "0123456789.") {
                if (negate) {
                    sb.append("-");
                }
                sb.append(currentCmd);
            }
        }

        var result = evaluateExpression(sb.toString());
        if (result == null) {
            textResult.text = "Error!";
        }
        else {
            textResult.text = result.toString();
        }
        totalString.clear();
        currentCmd.clear();
        clearNumber();
    }

    private fun maybeResolveTrailingOp() {
        if (currentCmd.isNotEmpty() && currentCmd[0] in "+-x/") {
            // we have trailing operation
            totalString.append(currentCmd.toString());
            currentCmd.clear();
        }
    }

    private fun clearNumber() {
        negate = false;
        seenDot = false;
    }

    private fun maybeResolveTrailingNum() {
        if (currentCmd.isNotEmpty() && currentCmd[0] in "0123456789.") {
            // we have trailing number
            if (negate) {
                totalString.append("-");
            }
            totalString.append(currentCmd.toString());
            currentCmd.clear();
            clearNumber();
        }
    }

    private fun addDigit(digit: Char) {
        maybeResolveTrailingOp();
        currentCmd.append(digit);
    }

    private fun maybeAddDot() {
        if (seenDot) {
            return;
        }
        maybeResolveTrailingOp();
        seenDot = true;
        currentCmd.append(".");
    }

    private fun addOp(op: Char) {
        maybeResolveTrailingNum();
        if (currentCmd.isNotEmpty()) {
            // we know it is trailing op here, so replace
            currentCmd.clear();
        }
        if (totalString.isNotEmpty() && totalString[totalString.length-1] in "+-x/") {
            // already have an op, do not add
            return;
        }
        currentCmd.append(op);
    }

    private fun maybeNegateCurrentNumber() {
        if (currentCmd.isNotEmpty() && currentCmd[0] in "0123456789.") {
            negate = !negate;
        }
    }

    private fun maybeDeleteOneChar() {
        if (currentCmd.isNotEmpty()) {
            currentCmd.setLength(currentCmd.length - 1);
        }
    }

    private fun maybeClearCurrentNumber() {
        if (currentCmd.isNotEmpty() && currentCmd[0] in "0123456789.") {
            currentCmd.clear();
            clearNumber();
        }
    }

    private fun resetCalculation() {
        totalString.clear();
        currentCmd.clear();
        clearNumber();
    }

    private fun addChar(digit: Char) {
        if (digit in "0123456789") {
            addDigit(digit);
        }
        else if (digit == '.') {
            maybeAddDot();
        }
        else if (digit in "+-x/") {
            addOp(digit);
        }
        else {
            // unresolved
        }

        updateText();
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        textResult  = findViewById<TextView>(R.id.textResult);
        textResult.movementMethod = ScrollingMovementMethod();
        textResult.setHorizontallyScrolling(true);

        val buttonDiv = findViewById<Button>(R.id.buttonDiv);
        val buttonBS = findViewById<Button>(R.id.buttonBS);
        val buttonC = findViewById<Button>(R.id.buttonC);
        val buttonCE = findViewById<Button>(R.id.buttonCE);
        val buttonMul = findViewById<Button>(R.id.buttonMul);
        val buttonD9 = findViewById<Button>(R.id.buttonD9);
        val buttonD8 = findViewById<Button>(R.id.buttonD8);
        val buttonD7 = findViewById<Button>(R.id.buttonD7);
        val buttonSub = findViewById<Button>(R.id.buttonSub);
        val buttonD6 = findViewById<Button>(R.id.buttonD6);
        val buttonD5 = findViewById<Button>(R.id.buttonD5);
        val buttonD4 = findViewById<Button>(R.id.buttonD4);
        val buttonAdd = findViewById<Button>(R.id.buttonAdd);
        val buttonD3 = findViewById<Button>(R.id.buttonD3);
        val buttonD2 = findViewById<Button>(R.id.buttonD2);
        val buttonD1 = findViewById<Button>(R.id.buttonD1);
        val buttonEqu = findViewById<Button>(R.id.buttonEqu);
        val buttonDot = findViewById<Button>(R.id.buttonDot);
        val buttonD0 = findViewById<Button>(R.id.buttonD0);
        val buttonNeg = findViewById<Button>(R.id.buttonNeg);

        buttonEqu.setOnClickListener({
            finalizeCalculation();
        })

        buttonBS.setOnClickListener({
            maybeDeleteOneChar();
            updateText();
        });

        buttonNeg.setOnClickListener({
            maybeNegateCurrentNumber();
            updateText();
        });

        buttonCE.setOnClickListener({
            maybeClearCurrentNumber();
            updateText();
        });

        buttonC.setOnClickListener({
            resetCalculation();
            updateText();
        });

        buttonD0.setOnClickListener({addChar('0')});
        buttonD1.setOnClickListener({addChar('1')});
        buttonD2.setOnClickListener({addChar('2')});
        buttonD3.setOnClickListener({addChar('3')});
        buttonD4.setOnClickListener({addChar('4')});
        buttonD5.setOnClickListener({addChar('5')});
        buttonD6.setOnClickListener({addChar('6')});
        buttonD7.setOnClickListener({addChar('7')});
        buttonD8.setOnClickListener({addChar('8')});
        buttonD9.setOnClickListener({addChar('9')});

        buttonAdd.setOnClickListener({addChar('+')});
        buttonSub.setOnClickListener({addChar('-')});
        buttonMul.setOnClickListener({addChar('x')});
        buttonDiv.setOnClickListener({addChar('/')});

        buttonDot.setOnClickListener({addChar('.')});
    }
}