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

@Grapes([
        @Grab("org.gebish:geb-core:2.1"),
        @Grab("org.seleniumhq.selenium:selenium-chrome-driver:3.6.0"),
        @Grab("org.seleniumhq.selenium:selenium-support:3.6.0")
])

import geb.Browser
import geb.navigator.Navigator
import groovy.io.FileType
import groovy.transform.Field

import java.time.LocalDateTime

//def parents(Navigator node){
//    def a = node
//    while (a) {
//        println a.tag() + ' - ' +a.classes()
//        a = a.parent()
//
//    }
//}

@Field
Map criteria = [
    onlyJournals: 2,
    journals: 1,
    conferences: 3
]

def findYear(text){
    for (x in 2018..1970) {
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

def min(... list){
    def min =  200000000
    list.each {
        if (it>max) max=it
        if (it<min) min=it
    }
    return min
}

def getYear(Navigator node){
    try {
        int year = Integer.parseInt(node.find('div b').text().substring(0, 4))
        return year
    }
    catch (Exception e) {return -1}

}

/**
 * Finds when a professor entered USP.
 *
 * @param node
 * @return [beginning date, ending date]
 */
def start(Navigator node){
    int begin = Integer.MAX_VALUE
    node.each{
        if (isICMC(it.find('b').text())){
            Navigator v = it.next().next().next()
            int year = getYear(v)
            if (year!=-1)
                begin = Math.min(begin, year)
            for (int i=0; i<2; i++) {
                v = v.next().next().next().next().next()
                year = getYear(v)
                if (year!=-1)
                    begin = Math.min(begin, year)
            }
        }
    }
    begin
}

/**
 * Count number of years in lst inside the closed interval [year-4 year]
 *
 * @param lst - List of years
 * @param year - last year: [year-4 year]
 * @return number or years in the interval
 */
//
def num(List lst, int year){
    int count = 0
    lst.each{if (it >= (year-4) && it <= year) ++count}
    count
}

/**
 * Finds the percentage of years that a professor fulfills the position criteria.
 *
 * @param prof - map with professor data
 * @param report - report file
 * @return
 */
def analysis(Map prof, a, b, report){
    int art = criteria.onlyJournals
    int confArt = criteria.journals
    int confConf = criteria.conferences
    println "Criterio: $art em periodicos OU $confArt em periodicos e $confConf em conferencias."
    report << "Criterio: $art em periodicos OU $confArt em periodicos e $confConf em conferencias.\n"

    int ok = 0
    for (x in a..b){
        //println("x $x")
        def journals = num(prof.articles, x)
        def confs = num(prof.confs, x)
        if (journals>=art || (journals>=confArt && confs>=confConf)) ++ok
        else {
            println "Nao atendeu em ${x-4} - $x [journals: $journals conf: $confs]."
            report << "Nao atendeu em ${x-4} - $x [journals: $journals conf: $confs].\n"
        }
    }
    (ok*1.0)/(b-a+1)
}

Browser.drive {

    def list = []

    //def dir = new File("/home/dilvan/Dropbox/IdeaProjects/webpageCorrector/src/main/resources")
    def dir = new File(args[0])
    dir.eachFileRecurse (FileType.FILES) { File file ->
        if (file.name.endsWith('.html'))
           list <<  URLEncoder.encode( file.name, "UTF-8").replace('+', '%20').replace('%28', '(').replace('%29', ')')
        println( file.name)
    }

    def report = new File('report.txt')
    if (report.exists())
        report.delete()

    report << LocalDateTime.now() as String
    report << '\n\n'

    for (filename in list) {

        println(URLDecoder.decode(filename))
        //go "file:///home/dilvan/Dropbox/IdeaProjects/webpageCorrector/src/main/resources/Curr%C3%ADculo%20do%20Sistema%20de%20Curr%C3%ADculos%20Lattes%20(Jos%C3%A9%20Fernando%20Rodrigues%20J%C3%BAnior).html"
        //go 'file:///home/dilvan/Dropbox/IdeaProjects/webpageCorrector/src/main/resources/Curr%C3%ADculo%20do%20Sistema%20de%20Curr%C3%ADculos%20Lattes%20(Dilvan%20de%20Abreu%20Moreira).html'
        //go 'file:///home/dilvan/Dropbox/IdeaProjects/webpageCorrector/src/main/resources/Curr%C3%ADculo%20do%20Sistema%20de%20Curr%C3%ADculos%20Lattes%20(Jo%C3%A3o%20Lu%C3%ADs%20Garcia%20Rosa).html'
        //go 'file:///home/dilvan/Dropbox/IdeaProjects/webpageCorrector/src/main/resources/Curr%C3%ADculo%20do%20Sistema%20de%20Curr%C3%ADculos%20Lattes%20(Rosane%20Minghim).html'
        //go 'file:///home/dilvan/Dropbox/IdeaProjects/webpageCorrector/src/main/resources/Curr%C3%ADculo%20do%20Sistema%20de%20Curr%C3%ADculos%20Lattes%20(Rudinei%20Goularte).html'

        go 'file:///home/dilvan/Dropbox/IdeaProjects/webpageCorrector/src/main/resources/'+filename

        def prof = [:]
        prof.name = $('h2.nome').text().split('\n')[0]
        prof.begin = start($('div.inst_back'))
        prof.articles = []
        prof.confs = []

        // Year that became an associate
        def ld = $('a[name$="LivreDocencia"]')
        if (!ld.empty)
            prof.associate = Integer.parseInt( ld.next().next().find('b', 0).text())
        else
            prof.associate = 2018

        report << prof.name + '\n'
        report << "Inicio Doutor: $prof.begin\n"
        report << "Fim Doutor: $prof.associate\n"

        $('#artigos-completos div.artigo-completo').each {
            if (it.find('sup img[src$="jcr.gif"]').empty) return
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
        //float per = 100 * analysis(prof.begin, prof.associate-5, 2017, report)
        float per = 100 * analysis(prof, prof.begin+4, prof.associate-1, report)

        //float per = 100 * analysis(prof, prof.associate, 2017, report)
        per = per.round(1)
        println "Percentagem do tempo que atende aos requisitos $per%.\n"
        report << "Percentagem do tempo que atende aos requisitos $per%.\n\n"
    }
}