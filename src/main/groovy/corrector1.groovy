/**
 *
 * Webpage Corrector
 *
 *
 * Copyright (C) 2018 Dilvan A. Moreira
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * The author can be contacted at dilvan@gmail.com
 *
 */

import geb.Browser
import geb.navigator.Navigator
import groovy.io.FileType

import java.time.LocalDateTime

def parents(Navigator node){
    def a = node
    while (a) {
        println a.tag() + ' - ' +a.classes()
        a = a.parent()

    }
}

def findYear(text){
    for (x in 2018..1980) {
        if (text.contains(' '+x+'.') || text.contains(' '+x+','))
            return x
    }
    -1
}

def isICMC(String text){
    text.contains('Instituto de Ciências Matemáticas e de Computação') ||
    text.contains('Universidade de São Paulo') ||
    text.contains('USP') ||
    text.contains('ICMC')
}

def minmax(... list){
    def max = -200000000
    def min =  200000000
    list.each {
        if (it>max) max=it
        if (it<min) min=it
    }
    return [min, max]
}

def period(node){
    int begin = 0
    int end = 2019
    node.each{
        if (isICMC(it.find('b').text())){

            int year = Integer.parseInt(it.next().next().next().find('div b').text().substring(0,4))
            if (begin==0) begin = year
            else {
                end = Math.max(begin, year)
                begin = Math.min(begin, year)
            }
            //println it.next().next().next().next().next().next().find('div b').text()
            try {
                if (it.next().next().next().next().next().next().find('div b').text().contains('Vínculo institucional')) {
                    int year2 = Integer.parseInt(it.next().next().next().next().next().next().next().next().find('div b').text().substring(0, 4))
                    if (year != year2) {
                        end = Math.max(begin, year2)
                        begin = Math.min(begin, year2)
                    }
                    try {
                        //println it.next().next().next().next().next().next().next().next().next().next().next().tag()

                        if (it.next().next().next().next().next().next().next().next().next().next().next().find('div b').text().contains('Vínculo institucional')) {
                            int year3 = Integer.parseInt(it.next().next().next().next().next().next().next().next().next().next().next().next().next().find('div b').text().substring(0, 4))
                            if (year3 != year && year3 != year2)
                                (begin, end) = minmax(year, year2, year3)
                        }
                    }
                    catch (Exception e) {}
                }
            }
            catch (Exception e) {}
        }
    }
    [begin, end]
}

def num(List lst, int year){
    int count = 0
    lst.each{if (it >= year && it < (year+5)) ++count}
    //println(count)
    count
}

def analysis(Map prof, report){
    int art = 2
    int confArt = 1
    int confConf =3
    println "Criterio: $art em periodicos OU $confArt em periodicos e $confConf em conferencias."
    report << "Criterio: $art em periodicos OU $confArt em periodicos e $confConf em conferencias.\n"

    int ok = 0
    for (x in prof.begin..(prof.end-5)){
        def articles = num(prof.articles, x)
        def confs = num(prof.confs, x)
        if (articles>=art || (articles>=confArt && confs>=confConf)) ++ok
        else {
            println "Nao atendeu em $x - ${x+4} [journals: $articles conf: $confs]."
            report << "Nao atendeu em $x - ${x+4} [journals: $articles conf: $confs].\n"
        }
    }
    (ok*1.0)/(prof.end - prof.begin - 4)
}

Browser.drive {

    def list = []

    def dir = new File("/home/dilvan/Dropbox/IdeaProjects/webpageCorrector/src/main/resources")
    dir.eachFileRecurse (FileType.FILES) { File file ->
        if (file.name.endsWith('.html'))
           list <<  URLEncoder.encode( file.name, "UTF-8").replace('+', '%20').replace('%28', '(').replace('%29', ')')
        println( file.name)
    }

    //println 'file:///home/dilvan/Dropbox/IdeaProjects/webpageCorrector/src/main/resources/'+list[0]
    //println 'file:///home/dilvan/Dropbox/IdeaProjects/webpageCorrector/src/main/resources/Curr%C3%ADculo%20do%20Sistema%20de%20Curr%C3%ADculos%20Lattes%20(Dilvan%20de%20Abreu%20Moreira).html'
    //assert title == "Geb - Very Groovy Browser Automation"

    def report = new File('report.txt')
    if (report.exists())
        report.delete()

    report << LocalDateTime.now() as String
    report << '\n\n'

    //println $('h2.nome', 0).text()
    for (filename in list) {

        println(URLDecoder.decode(filename))
        //go "file:///home/dilvan/Dropbox/IdeaProjects/webpageCorrector/src/main/resources/Curr%C3%ADculo%20do%20Sistema%20de%20Curr%C3%ADculos%20Lattes%20(Jos%C3%A9%20Fernando%20Rodrigues%20J%C3%BAnior).html"
        //go 'file:///home/dilvan/Dropbox/IdeaProjects/webpageCorrector/src/main/resources/Curr%C3%ADculo%20do%20Sistema%20de%20Curr%C3%ADculos%20Lattes%20(Dilvan%20de%20Abreu%20Moreira).html'
        //assert title == "Geb - Very Groovy Browser Automation"
        //go 'file:///home/dilvan/Dropbox/IdeaProjects/webpageCorrector/src/main/resources/Curr%C3%ADculo%20do%20Sistema%20de%20Curr%C3%ADculos%20Lattes%20(Jo%C3%A3o%20Lu%C3%ADs%20Garcia%20Rosa).html'

        go 'file:///home/dilvan/Dropbox/IdeaProjects/webpageCorrector/src/main/resources/'+filename

        def prof = [:]
        prof.name = $('h2.nome').text().split('\n')[0]
        def (begin, end) = period($('div.inst_back'))
        prof.begin = begin
        prof.end = end
        prof.articles = []
        prof.confs = []

        report << prof.name + '\n'
        report << "Inicio Doutor: $prof.begin\n"
        report << "Fim Doutor: $prof.end\n"

        $("#artigos-completos").find('div.artigo-completo').each {
            if (it.find('sup img[src$="jcr.gif"]').empty) return
            //def art = [:]
            //art.year = it.find('span[data-tipo-ordenacao="ano"]').text()
            // art.jcr = !it.find('sup img[src$="jcr.gif"]').empty
            prof.articles << Integer.parseInt(it.find('span[data-tipo-ordenacao="ano"]').text())
        }

        def conf
        $("div.cita-artigos").each {
            if (it.find('b').text().contains('Trabalhos completos publicados em anais de congressos'))
                conf = it
        }
        println conf.text()
        conf = conf.next()
        while (conf.getAttribute("class") != 'cita-artigos') {
            def text = conf.find('div', 0).text()
            if (text && text.size() > 10) {
                //println( text)
                if (text.contains('Classificação do evento: Internacional')) {
                    //println(text+ ' - '+ findYear(text))
                    prof.confs << findYear(text)
                }
            }
            conf = conf.next()
        }
        report << "Periodicos: $prof.articles\n"
        report << "Conferencias: $prof.confs\n"

        println(prof)

        float per = 100 * analysis(prof, report)
        per = per.round(1)
        println "Percentagem do tempo que atende aos requisitos $per%.\n"
        report << "Percentagem do tempo que atende aos requisitos $per%.\n\n"
    }
//    println $("#artigos-completos").find("div.artigo-completo").find('span',3).text() //.getAttribute("data-tipo-ordenacao")
//    parents($("#artigos-completos").find("div.artigo-completo").find('span',3))
//    println $("div.combo h1", 0).text()
//    println $("span.informacao-artigo")
//    waitFor { !$("#manuals-m").hasClass("animating") }
//
//    $("#manuals-menu a")[0].click()
//

//    assert title.startsWith("The Book Of Geb")
}