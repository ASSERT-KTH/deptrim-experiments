library(tidyverse)
library(latex2exp)
library(scales)
library(extrafont)
library(viridis)
library(ggridges)
library(forcats)
library(beanplot)
library(venn)
library(xtable)
library(ggplot2)
library(ggpolypath)

# set fonts
loadfonts(device = "pdf")
# font_import()
# link www.fontsquirrel.com/fonts/latin-modern-roman

# execute once to add fonts:
# font_import(pattern = "lmroman*")
# theme(legend.position = "top", text=element_text(size=14, family="LM Roman 10"))

# set ggplot global theme
theme_set(theme_bw() +
            theme(legend.position = "top") +
            theme(text = element_text(size = 18, family = "LM Roman 10")))

# multiple figures together
if(!require(devtools)) install.packages("devtools")
# devtools::install_github("thomasp85/patchwork")
library(patchwork)
library(tikzDevice)
options(tz="CA")

# Colors
# green: #009d81
# yellow: #f5a300
# gray: #808080
# red: #e6001a
# blue: #0083cc

# Best Colors (https://github.com/OrdnanceSurvey/GeoDataViz-Toolkit/blob/master/Colours/GDV%20colour%20palettes%200.7.pdf)
# green: #00cd6c
# yellow: #ffc61e
# gray: #a0b1ba
# red: #ff1f5d
# blue: #009ade
# purple: #af58ba
# orange: #f28522
# brown: #a6761d


###################################################
#################### FUNCTIONS ####################
###################################################

save_figure <- function(the_ggplot_object, the_name, the_with, the_height){
  tikz(file = paste(the_name, "tex", sep = "." ), width = the_with, height = the_height)
  print(the_ggplot_object)
  dev.off()
  ggsave(filename = paste(the_name, "pdf", sep = "." ), 
         plot = the_ggplot_object, 
         height = the_height,
         width = the_with, 
         units = c("in"),
         device=cairo_pdf)
}