

app = Flask(__name__)

@app.route("/")
def home():
    return "<h1>สวัสดี! นี่คือเว็บแรกของฉัน</h1>"

@app.route("/about")
def about():
    return "<p>หน้านี้คือ About Page</p>"

if __name__ == "__main__":
    app.run(debug=True)