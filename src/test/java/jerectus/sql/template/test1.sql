select /*@user*/ USER.*
  from USER
 where ID = /*:id?*/1
   and (
       --#for name : names ; delim=' or '
       NAME like /*:name%*/'a%'
       --#end-for
       )
 order by
       ID   --##if orderBy == 1
     , NAME --##if orderBy == 2
