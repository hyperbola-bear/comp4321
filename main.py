from bs4 import BeautifulSoup
import requests

def remove_until_backslash(url):
    for i in range(len(url) - 1, -1, -1):
        if url[i] == '/':
            return url[:i + 1]
    return url

def beginswith(string, prefix):
    if len(string) < len(prefix):
        return False
    for i in range(len(prefix)):
        if string[i] != prefix[i]:
            return False
    return True

queue = []
queue.append('https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm')
visited = {}
visited['https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm'] = 1
page_count = 0

# create a file to store the visited pages content
file = open("spider_result.txt", "w")

while len(queue) > 0 and page_count < 5:
    page_count += 1
    url = queue.pop(0)
    print("current page: ", url)
    html_text = requests.get(url)
    soup = BeautifulSoup(html_text.text, 'html.parser')
    title = soup.title.string
    file.write(title + '\n')
    file.write(url + '\n')
    last_modified = html_text.headers['last-modified']
    size = html_text.headers['content-length']
    file.write(last_modified + ',' + size + '\n')
    words_dict = {}
    for word in soup.get_text().split():
        word = word.lower()
        if word in words_dict:
            words_dict[word] += 1
        else:
            words_dict[word] = 1
    for word in words_dict:
        file.write(word + ':' + str(words_dict[word]) + '; ')
    file.write('\n')
    # print(soup.prettify())
    base_url = remove_until_backslash(url)
    for link in soup.find_all('a'):
        link = link.get('href')
        if link is not None and base_url + link not in visited and not beginswith(link, '../'):
            queue.append(base_url + link)
            print('page added: ', base_url + link)
            file.write(base_url + link + '\n')
            visited[base_url + link] = 1
    file.write('---------------------------------------------------------------' + '\n')
    
print('Total pages: ', page_count)