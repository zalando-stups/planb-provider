#!/usr/bin/env python3

import flask

app = flask.Flask(__name__)


@app.route('/ws/customerService', methods=['GET'])
def wsdl():
    with open('wsdl') as fd:
        contents = fd.read()
    host = flask.request.headers['Host']
    is_local = host == 'localhost'
    location = '{}://{}{}/ws/customerService'.format('http' if is_local else 'https', host, ':8080' if is_local else '')
    return contents.replace('{{location}}', location)


@app.route('/ws/customerService', methods=['POST'])
def authenticate():
    '''<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body><ns2:authenticate xmlns:ns2="http://service.webservice.user.zalando.de/"><email>jdoe@example.org</email><password>foo</pas
    sword></ns2:authenticate></soap:Body></soap:Envelope>'''
    response = '''<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
    <soap:Body><ns2:authenticateResponse xmlns:ns2="http://service.webservice.customer.zalando.de/" xmlns:ns3="http://ws.zalando.de/bm/shop">
    <return><appDomainId>1</appDomainId><customerNumber>123</customerNumber><firstname>John</firstname>
    <gender>MALE</gender><lastname>Doe</lastname><loginResult>SUCCESS</loginResult></return></ns2:authenticateResponse></soap:Body></soap:Envelope>'''
    return response

if __name__ == "__main__":
    app.run(host='0.0.0.0', port=8080)
